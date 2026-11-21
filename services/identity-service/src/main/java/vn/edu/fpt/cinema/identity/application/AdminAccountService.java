package vn.edu.fpt.cinema.identity.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.domain.entity.PrincipalRole;
import vn.edu.fpt.cinema.identity.domain.entity.Staff;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;
import vn.edu.fpt.cinema.identity.dto.AccountResponse;
import vn.edu.fpt.cinema.identity.dto.AdminResetPasswordRequest;
import vn.edu.fpt.cinema.identity.dto.UpdateAccountRolesRequest;
import vn.edu.fpt.cinema.identity.dto.UpdateAccountStatusRequest;
import vn.edu.fpt.cinema.identity.repository.CustomerRepository;
import vn.edu.fpt.cinema.identity.repository.PrincipalRepository;
import vn.edu.fpt.cinema.identity.repository.StaffRepository;

@Service
public class AdminAccountService {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountService.class);

    private final PrincipalRepository principalRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(
            PrincipalRepository principalRepository,
            CustomerRepository customerRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder) {
        this.principalRepository = principalRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccounts(
            String keyword,
            AccountType accountType,
            RoleType role,
            Boolean isActive,
            int page,
            int size) {

        int validSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String searchKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Principal> principalPage = principalRepository.searchPrincipals(searchKeyword, accountType, isActive, pageable);

        List<AccountResponse> responses = principalPage.getContent().stream()
                .filter(p -> {
                    if (role == null) return true;
                    return p.getRoles().stream().anyMatch(r -> r.getRoleName() == role);
                })
                .map(this::mapToAccountResponse)
                .toList();

        return new PageImpl<>(responses, pageable, principalPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountDetail(String principalId) {
        Principal principal = principalRepository.findById(principalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with ID: " + principalId));
        return mapToAccountResponse(principal);
    }

    @Transactional
    public AccountResponse updateAccountStatus(
            String actorPrincipalId,
            String targetPrincipalId,
            UpdateAccountStatusRequest request) {

        Principal target = principalRepository.findById(targetPrincipalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with ID: " + targetPrincipalId));

        // Privilege Escalation Rule: Prevent admin self-deactivation (anti-lockout)
        if (targetPrincipalId.equalsIgnoreCase(actorPrincipalId) && Boolean.FALSE.equals(request.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot deactivate their own account.");
        }

        target.setIsActive(request.getIsActive());

        // Sync state with Customer or Staff
        if (target.getCustomerId() != null) {
            customerRepository.findById(target.getCustomerId()).ifPresent(c -> {
                c.setVerify(request.getIsActive() ? "active" : "locked");
                customerRepository.save(c);
            });
        }
        if (target.getStaffId() != null) {
            staffRepository.findById(target.getStaffId()).ifPresent(staffRepository::save);
        }

        principalRepository.save(target);

        log.info("[AdminAccount] Account '{}' status changed to isActive={} by admin '{}'. Reason: {}",
                target.getUsername(), request.getIsActive(), actorPrincipalId, request.getReason());

        return mapToAccountResponse(target);
    }

    @Transactional
    public AccountResponse updateAccountRoles(
            String actorPrincipalId,
            String targetPrincipalId,
            UpdateAccountRolesRequest request) {

        Principal target = principalRepository.findById(targetPrincipalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with ID: " + targetPrincipalId));

        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An account must have at least one role.");
        }

        // Whitelist role validation against RoleType enum
        Set<RoleType> parsedRoles = new HashSet<>();
        for (String roleStr : request.getRoles()) {
            if (roleStr == null || roleStr.isBlank()) {
                continue;
            }
            try {
                RoleType rt = RoleType.valueOf(roleStr.trim().toUpperCase());
                parsedRoles.add(rt);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid role specified: '" + roleStr + "'. Allowed roles are: USER, CASHIER_STAFF, REDEMPTION_STAFF, ADMIN");
            }
        }

        if (parsedRoles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An account must have at least one valid role.");
        }

        // Privilege Escalation Rule: Prevent admin self-demotion (anti-lockout)
        if (targetPrincipalId.equalsIgnoreCase(actorPrincipalId) && !parsedRoles.contains(RoleType.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot remove their own ADMIN role.");
        }

        // Privilege Escalation Rule: Prevent customer accounts from acquiring staff/admin roles directly
        if (target.getAccountType() == AccountType.CUSTOMER) {
            boolean hasElevatedRole = parsedRoles.contains(RoleType.ADMIN)
                    || parsedRoles.contains(RoleType.CASHIER_STAFF)
                    || parsedRoles.contains(RoleType.REDEMPTION_STAFF);

            if (hasElevatedRole && target.getStaffId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot assign internal staff or administrative roles to a customer account directly.");
            }
        }

        // Apply role updates
        target.getRoles().clear();
        for (RoleType rt : parsedRoles) {
            target.getRoles().add(new PrincipalRole(UUID.randomUUID().toString(), target, rt));
        }

        principalRepository.save(target);

        log.info("[AdminAccount] Account '{}' roles updated to {} by admin '{}'",
                target.getUsername(), parsedRoles, actorPrincipalId);

        return mapToAccountResponse(target);
    }

    @Transactional
    public Map<String, Object> adminResetPassword(
            String actorPrincipalId,
            String targetPrincipalId,
            AdminResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The confirmation passwords do not match.");
        }

        Principal target = principalRepository.findById(targetPrincipalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found with ID: " + targetPrincipalId));

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        target.setPasswordHash(encodedPassword);

        // Sync with domain entities
        if (target.getCustomerId() != null) {
            customerRepository.findById(target.getCustomerId()).ifPresent(c -> {
                c.setPasswordHash(encodedPassword);
                customerRepository.save(c);
            });
        }
        if (target.getStaffId() != null) {
            staffRepository.findById(target.getStaffId()).ifPresent(s -> {
                s.setPasswordHash(encodedPassword);
                staffRepository.save(s);
            });
        }

        principalRepository.save(target);

        log.info("[AdminAccount] Password forcefully reset for account '{}' by admin '{}'",
                target.getUsername(), actorPrincipalId);

        return Map.of(
                "message", "Password reset successfully for account: " + target.getUsername(),
                "principalId", target.getPrincipalId()
        );
    }

    public AccountResponse mapToAccountResponse(Principal principal) {
        String fullName = principal.getUsername();
        String email = null;
        String phone = null;
        String domainId = null;

        if (principal.getCustomerId() != null) {
            domainId = principal.getCustomerId();
            Customer customer = customerRepository.findById(domainId).orElse(null);
            if (customer != null) {
                fullName = customer.getUsername();
                email = customer.getEmail();
                phone = customer.getPhone();
            }
        } else if (principal.getStaffId() != null) {
            domainId = principal.getStaffId();
            Staff staff = staffRepository.findById(domainId).orElse(null);
            if (staff != null) {
                fullName = staff.getFullName();
                email = staff.getEmail();
                phone = staff.getPhone();
            }
        }

        List<RoleType> roles = new ArrayList<>(principal.getRoles().stream()
                .map(PrincipalRole::getRoleName)
                .sorted()
                .toList());

        return new AccountResponse(
                principal.getPrincipalId(),
                principal.getUsername(),
                principal.getAccountType(),
                fullName,
                email,
                phone,
                roles,
                principal.getIsActive(),
                domainId,
                principal.getCreatedAt(),
                principal.getUpdatedAt()
        );
    }
}
