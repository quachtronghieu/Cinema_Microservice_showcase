package vn.edu.fpt.cinema.identity.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cinema.identity.domain.entity.Customer;
import vn.edu.fpt.cinema.identity.domain.entity.Principal;
import vn.edu.fpt.cinema.identity.domain.entity.PrincipalRole;
import vn.edu.fpt.cinema.identity.domain.entity.Staff;
import vn.edu.fpt.cinema.identity.domain.enums.AccountType;
import vn.edu.fpt.cinema.identity.domain.enums.RoleType;
import vn.edu.fpt.cinema.identity.repository.CustomerRepository;
import vn.edu.fpt.cinema.identity.repository.PrincipalRepository;
import vn.edu.fpt.cinema.identity.repository.PrincipalRoleRepository;
import vn.edu.fpt.cinema.identity.repository.StaffRepository;

@Service
public class AccountMigrationService {

    private final PrincipalRepository principalRepository;
    private final PrincipalRoleRepository principalRoleRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountMigrationService(
            PrincipalRepository principalRepository,
            PrincipalRoleRepository principalRoleRepository,
            CustomerRepository customerRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder) {
        this.principalRepository = principalRepository;
        this.principalRoleRepository = principalRoleRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> auditMigrationStatus() {
        Map<String, Object> audit = new HashMap<>();

        long totalCustomers = customerRepository.count();
        long totalStaffs = staffRepository.count();
        long totalPrincipals = principalRepository.count();
        long customerPrincipals = principalRepository.countByAccountType(AccountType.CUSTOMER);
        long staffPrincipals = principalRepository.countByAccountType(AccountType.STAFF);

        long userRoles = principalRoleRepository.countByRoleName(RoleType.USER);
        long adminRoles = principalRoleRepository.countByRoleName(RoleType.ADMIN);
        long cashierRoles = principalRoleRepository.countByRoleName(RoleType.CASHIER_STAFF);
        long redemptionRoles = principalRoleRepository.countByRoleName(RoleType.REDEMPTION_STAFF);

        audit.put("total_legacy_customers", totalCustomers);
        audit.put("total_legacy_staffs", totalStaffs);
        audit.put("total_migrated_principals", totalPrincipals);
        audit.put("customer_principals", customerPrincipals);
        audit.put("staff_principals", staffPrincipals);
        audit.put("role_distribution", Map.of(
                "USER", userRoles,
                "ADMIN", adminRoles,
                "CASHIER_STAFF", cashierRoles,
                "REDEMPTION_STAFF", redemptionRoles
        ));

        boolean customerReconciled = (totalCustomers == customerPrincipals);
        boolean staffReconciled = (totalStaffs == staffPrincipals);
        audit.put("status", (customerReconciled && staffReconciled) ? "RECONCILED_SUCCESS" : "RECONCILIATION_REQUIRED");

        return audit;
    }

    @Transactional
    public void reconcileCustomerAccounts() {
        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            String normalizedUsername = customer.getUsername().trim().toLowerCase();
            if (!principalRepository.existsByNormalizedUsername(normalizedUsername)) {
                String rawPassword = customer.getPasswordHash();
                String secureHash = (rawPassword != null && rawPassword.startsWith("$2a$"))
                        ? rawPassword
                        : passwordEncoder.encode(rawPassword != null ? rawPassword : "default123");

                Principal principal = new Principal(
                        "p-cust-" + customer.getUserId(),
                        customer.getUsername(),
                        normalizedUsername,
                        secureHash,
                        AccountType.CUSTOMER
                );
                principal.setCustomerId(customer.getUserId());
                principalRepository.save(principal);

                PrincipalRole role = new PrincipalRole(
                        "pr-role-" + customer.getUserId() + "-user",
                        principal,
                        RoleType.USER
                );
                principalRoleRepository.save(role);
            }
        }
    }
}
