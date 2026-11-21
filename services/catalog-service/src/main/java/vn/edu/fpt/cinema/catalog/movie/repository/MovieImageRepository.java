package vn.edu.fpt.cinema.catalog.movie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.cinema.catalog.movie.entity.MovieImage;

public interface MovieImageRepository extends JpaRepository<MovieImage,String> {}
