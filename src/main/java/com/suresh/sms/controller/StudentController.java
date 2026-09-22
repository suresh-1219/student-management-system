package com.suresh.sms.controller;

import java.math.BigDecimal;
import java.util.List;


import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suresh.sms.dto.ApiResponse;
import com.suresh.sms.dto.PageResponse;
import com.suresh.sms.dto.StudentDTO;
import com.suresh.sms.entity.Student;
import com.suresh.sms.service.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/students")
@SecurityRequirement(name = "Bearer Authentication")
public class StudentController {

    @Autowired
    private StudentService service;


   
    // CREATE STUDENT


    @PostMapping
    public ResponseEntity<ApiResponse<StudentDTO>> saveStudent(
            @Valid @RequestBody StudentDTO dto) {

        StudentDTO savedStudent =
                service.saveStudent(dto);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student created successfully",
                        savedStudent
                )
        );
    }


  
    // GET ALL STUDENTS


    @Deprecated
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getAllStudents() {

        List<StudentDTO> students =
                service.getAllStudents();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Students retrieved successfully",
                        students
                )
        );
    }


    
    // GET STUDENT BY ID

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO>> getStudentById(
            @PathVariable Long id) {

        StudentDTO student =
                service.getStudentById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student retrieved successfully",
                        student
                )
        );
    }



    // SEARCH STUDENT BY NAME
    

    @Deprecated
    @GetMapping("/search/{name}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getStudentByName(
            @PathVariable String name) {

        List<StudentDTO> students = service.getStudentByName(name);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students found successfully", students));
    }


  
    // SEARCH + FILTER + SORT + PAGE (use this instead of the deprecated list endpoints)

    @Operation(
            summary = "Search, filter, sort and page students",
            description = "All parameters are optional. name = partial, case-insensitive match; "
                    + "course = exact, case-insensitive match; minFee / maxFee = inclusive range. "
                    + "sort: id, name, email, course or fee. direction: asc or desc. "
                    + "size: 1-100.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StudentDTO>>> searchStudents(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "course", required = false) String course,
            @RequestParam(name = "minFee", required = false) BigDecimal minFee,
            @RequestParam(name = "maxFee", required = false) BigDecimal maxFee,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "direction", defaultValue = "asc") String direction) {

        PageResponse<StudentDTO> result =
                service.searchStudents(name, course, minFee, maxFee, page, size, sort, direction);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Students retrieved successfully",
                        result
                )
        );
    }


    // PAGINATION
 

    @Deprecated
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<StudentDTO>>> getStudents(
            @RequestParam int page,
            @RequestParam int size) {

        Page<StudentDTO> students =
                service.getStudents(page, size);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Students retrieved successfully",
                        students
                )
        );
    }


    
    // SORT ASCENDING
   

    @Deprecated
    @GetMapping("/sort/{field}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> sortStudents(
            @PathVariable String field) {

        List<StudentDTO> students = service.sortStudents(field);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students sorted successfully", students));
    }

 
    // SORT DESCENDING
   

    @Deprecated
    @GetMapping("/sortDesc/{field}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> sortStudentsDesc(
            @PathVariable String field) {

        List<StudentDTO> students = service.sortStudentsDesc(field);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students sorted successfully in descending order", students));
    }
    
    // PAGINATION + SORTING
    

    @Deprecated
    @GetMapping("/pageSort")
    public ResponseEntity<ApiResponse<Page<StudentDTO>>>
    getStudentsWithPaginationAndSorting(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String field) {

        Page<StudentDTO> students =
                service.getStudentsWithPaginationAndSorting(page, size, field);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Students retrieved with pagination and sorting", students));
    }
   
    // UPDATE STUDENT
    

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO dto) {

        StudentDTO updatedStudent =
                service.updateStudent(id, dto);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student updated successfully",
                        updatedStudent
                )
        );
    }


    
    // DELETE STUDENT


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStudent(
            @PathVariable Long id) {

        service.deleteStudent(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Student deleted successfully",
                        null
                )
        );
    }
}