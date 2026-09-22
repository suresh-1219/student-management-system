package com.suresh.sms.service;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.suresh.sms.dto.PageResponse;
import com.suresh.sms.dto.StudentDTO;
import com.suresh.sms.entity.Student;
import com.suresh.sms.exception.DuplicateStudentException;
import com.suresh.sms.exception.InvalidRequestException;
import com.suresh.sms.exception.StudentNotFoundException;
import com.suresh.sms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private StudentService studentService;


   
    // GET ALL STUDENTS
    
    @Test
    void testGetAllStudents() {

        Student student1 = new Student();
        student1.setId(1L);
        student1.setName("Suresh");
        student1.setEmail("suresh@gmail.com");
        student1.setCourse("MCA");
        student1.setFee(BigDecimal.valueOf(50000.0));

        Student student2 = new Student();
        student2.setId(2L);
        student2.setName("Rahul");
        student2.setEmail("rahul@gmail.com");
        student2.setCourse("B.Tech");
        student2.setFee(BigDecimal.valueOf(45000.0));

        StudentDTO dto1 = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        StudentDTO dto2 = new StudentDTO(
                2L,
                "Rahul",
                "rahul@gmail.com",
                "B.Tech",
                BigDecimal.valueOf(45000.0)
        );

        when(repository.findAll())
                .thenReturn(Arrays.asList(student1, student2));

        when(modelMapper.map(student1, StudentDTO.class))
                .thenReturn(dto1);

        when(modelMapper.map(student2, StudentDTO.class))
                .thenReturn(dto2);

        List<StudentDTO> result =
                studentService.getAllStudents();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Suresh", result.get(0).getName());
        assertEquals("Rahul", result.get(1).getName());
    }


    
    // GET STUDENT BY ID - SUCCESS
    
    @Test
    void testGetStudentByIdSuccess() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(student));

        when(modelMapper.map(student, StudentDTO.class))
                .thenReturn(dto);

        StudentDTO result =
                studentService.getStudentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Suresh", result.getName());
        assertEquals("suresh@gmail.com", result.getEmail());
    }


    
    // GET STUDENT BY ID - NOT FOUND
    
    @Test
    void testGetStudentByIdNotFound() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        StudentNotFoundException exception =
                assertThrows(
                        StudentNotFoundException.class,
                        () -> studentService.getStudentById(99L)
                );

        assertEquals(
                "Student not found with id : 99",
                exception.getMessage()
        );
    }


    
    // SAVE STUDENT
   
    @Test
    void testSaveStudent() {

        StudentDTO dto = new StudentDTO(
                null,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        Student student = new Student();

        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        Student savedStudent = new Student();

        savedStudent.setId(10L);
        savedStudent.setName("Suresh");
        savedStudent.setEmail("suresh@gmail.com");
        savedStudent.setCourse("MCA");
        savedStudent.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO resultDTO = new StudentDTO(
                10L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        when(modelMapper.map(dto, Student.class))
                .thenReturn(student);

        when(repository.save(student))
                .thenReturn(savedStudent);

        when(modelMapper.map(savedStudent, StudentDTO.class))
                .thenReturn(resultDTO);

        StudentDTO result =
                studentService.saveStudent(dto);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Suresh", result.getName());
        assertEquals("MCA", result.getCourse());
        assertEquals(BigDecimal.valueOf(50000.0), result.getFee());
    }


    
    // UPDATE STUDENT - SUCCESS
    
    @Test
    void testUpdateStudentSuccess() {

        Student existing = new Student();

        existing.setId(1L);
        existing.setName("Old Name");
        existing.setEmail("old@gmail.com");
        existing.setCourse("BCA");
        existing.setFee(BigDecimal.valueOf(40000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh Kumar",
                "sureshkumar@gmail.com",
                "MCA",
                BigDecimal.valueOf(55000.0)
        );

        StudentDTO resultDTO = new StudentDTO(
                1L,
                "Suresh Kumar",
                "sureshkumar@gmail.com",
                "MCA",
                BigDecimal.valueOf(55000.0)
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(repository.save(existing))
                .thenReturn(existing);

        when(modelMapper.map(existing, StudentDTO.class))
                .thenReturn(resultDTO);

        StudentDTO result =
                studentService.updateStudent(1L, dto);

        assertNotNull(result);

        assertEquals(
                "Suresh Kumar",
                result.getName()
        );

        assertEquals(
                "sureshkumar@gmail.com",
                result.getEmail()
        );

        assertEquals(
                "MCA",
                result.getCourse()
        );

        assertEquals(
                BigDecimal.valueOf(55000.0),
                result.getFee()
        );
    }


    
    // UPDATE STUDENT - NOT FOUND
    
    @Test
    void testUpdateStudentNotFound() {

        StudentDTO dto = new StudentDTO(
                99L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        StudentNotFoundException exception =
                assertThrows(
                        StudentNotFoundException.class,
                        () -> studentService.updateStudent(99L, dto)
                );

        assertEquals(
                "Student not found with id : 99",
                exception.getMessage()
        );
    }


    
    // DELETE STUDENT - SUCCESS
   
    @Test
    void testDeleteStudentSuccess() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        when(repository.findById(1L))
                .thenReturn(Optional.of(student));

        studentService.deleteStudent(1L);

        verify(repository).delete(student);
    }


    
    // DELETE STUDENT - NOT FOUND
    
    @Test
    void testDeleteStudentNotFound() {

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        StudentNotFoundException exception =
                assertThrows(
                        StudentNotFoundException.class,
                        () -> studentService.deleteStudent(99L)
                );

        assertEquals(
                "Student not found with id : 99",
                exception.getMessage()
        );
    }


    
    // SEARCH STUDENT BY NAME
    
    @Test
    void testGetStudentByName() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        when(repository.findByName("Suresh"))
                .thenReturn(List.of(student));

        when(modelMapper.map(student, StudentDTO.class))
                .thenReturn(dto);

        List<StudentDTO> result =
                studentService.getStudentByName("Suresh");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Suresh", result.get(0).getName());
    }

   
    // SORT STUDENTS ASCENDING
    
    @Test
    void testSortStudents() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(student));

        when(modelMapper.map(student, StudentDTO.class))
                .thenReturn(dto);

        List<StudentDTO> result =
                studentService.sortStudents("name");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Suresh", result.get(0).getName());
    }


    
    // SORT STUDENTS DESCENDING

    @Test
    void testSortStudentsDesc() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(student));

        when(modelMapper.map(student, StudentDTO.class))
                .thenReturn(dto);

        List<StudentDTO> result =
                studentService.sortStudentsDesc("name");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Suresh", result.get(0).getName());
    }


    
    // PAGINATION
    
    @Test
    void testGetStudents() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        Page<Student> page =
                new PageImpl<>(List.of(student));

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(page);

        when(modelMapper.map(student, StudentDTO.class))
                .thenReturn(dto);

        Page<StudentDTO> result =
                studentService.getStudents(0, 5);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(
                "Suresh",
                result.getContent().get(0).getName()
        );
    }


    
    // PAGINATION + SORTING
   
    @Test
    void testGetStudentsWithPaginationAndSorting() {

        Student student = new Student();

        student.setId(1L);
        student.setName("Suresh");
        student.setEmail("suresh@gmail.com");
        student.setCourse("MCA");
        student.setFee(BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L,
                "Suresh",
                "suresh@gmail.com",
                "MCA",
                BigDecimal.valueOf(50000.0)
        );

        Page<Student> page =
                new PageImpl<>(List.of(student));

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(page);

        when(modelMapper.map(student, StudentDTO.class))
                .thenReturn(dto);

        Page<StudentDTO> result =
                studentService.getStudentsWithPaginationAndSorting(
                        0,
                        5,
                        "name"
                );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(
                "Suresh",
                result.getContent().get(0).getName()
        );
    }

    // SORT FIELD WHITELIST

    @Test
    void testSortRejectsUnknownField() {

        assertThrows(InvalidRequestException.class,
                () -> studentService.sortStudents("password"));

        assertThrows(InvalidRequestException.class,
                () -> studentService.sortStudentsDesc("nonexistent"));

        assertThrows(InvalidRequestException.class,
                () -> studentService.sortStudents(null));

        verifyNoInteractions(repository);
    }

    @Test
    void testSortAcceptsEveryAllowedField() {

        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of());

        for (String field : List.of("id", "name", "email", "course", "fee")) {
            assertEquals(0, studentService.sortStudents(field).size());
            assertEquals(0, studentService.sortStudentsDesc(field).size());
        }
    }

    @Test
    void testPageSortRejectsUnknownField() {

        assertThrows(InvalidRequestException.class,
                () -> studentService.getStudentsWithPaginationAndSorting(0, 5, "bogus"));

        verifyNoInteractions(repository);
    }


    // PAGE / SIZE LIMITS

    @Test
    void testPagingRejectsNegativePage() {

        assertThrows(InvalidRequestException.class,
                () -> studentService.getStudents(-1, 10));

        verifyNoInteractions(repository);
    }

    @Test
    void testPagingRejectsSizeOutOfRange() {

        assertThrows(InvalidRequestException.class,
                () -> studentService.getStudents(0, 0));

        assertThrows(InvalidRequestException.class,
                () -> studentService.getStudents(0, 101));

        assertThrows(InvalidRequestException.class,
                () -> studentService.getStudentsWithPaginationAndSorting(0, 1_000_000, "name"));

        verifyNoInteractions(repository);
    }

    @Test
    void testPagingAcceptsBoundaryValues() {

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, studentService.getStudents(0, 1).getTotalElements());
        assertEquals(0, studentService.getStudents(0, 100).getTotalElements());
    }

    // DUPLICATE STUDENT E-MAIL

    private StudentDTO dtoWithEmail(String email) {

        StudentDTO dto = new StudentDTO();
        dto.setName("Suresh");
        dto.setEmail(email);
        dto.setCourse("MCA");
        dto.setFee(BigDecimal.valueOf(50000.0));

        return dto;
    }

    @Test
    void testSaveStudentRejectsDuplicateEmail() {

        when(repository.existsByEmail("taken@gmail.com")).thenReturn(true);

        DuplicateStudentException ex = assertThrows(
                DuplicateStudentException.class,
                () -> studentService.saveStudent(dtoWithEmail("taken@gmail.com")));

        assertEquals("A student with this email already exists", ex.getMessage());

        verify(repository, org.mockito.Mockito.never()).save(any(Student.class));
    }

    @Test
    void testUpdateStudentRejectsEmailOfAnotherStudent() {

        Student existing = new Student(1L, "Old", "old@gmail.com", "MCA", BigDecimal.valueOf(1000.0));

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("taken@gmail.com", 1L)).thenReturn(true);

        assertThrows(
                DuplicateStudentException.class,
                () -> studentService.updateStudent(1L, dtoWithEmail("taken@gmail.com")));

        verify(repository, org.mockito.Mockito.never()).save(any(Student.class));
    }

    @Test
    void testUpdateStudentMayKeepItsOwnEmail() {

        Student existing = new Student(1L, "Old", "same@gmail.com", "MCA", BigDecimal.valueOf(1000.0));

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("same@gmail.com", 1L)).thenReturn(false);
        when(repository.save(existing)).thenReturn(existing);

        studentService.updateStudent(1L, dtoWithEmail("same@gmail.com"));

        verify(repository).save(existing);
    }

    // SEARCH

    @Test
    void testSearchRejectsInvalidDirection() {

        InvalidRequestException ex = assertThrows(
                InvalidRequestException.class,
                () -> studentService.searchStudents(
                        null, null, null, null, 0, 10, "name", "sideways"));

        assertEquals("Invalid direction. Allowed values: asc, desc", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void testSearchRejectsMinFeeGreaterThanMaxFee() {

        assertThrows(
                InvalidRequestException.class,
                () -> studentService.searchStudents(
                        null, null,
                        BigDecimal.valueOf(5000), BigDecimal.valueOf(1000),
                        0, 10, "id", "asc"));

        verifyNoInteractions(repository);
    }

    @Test
    void testSearchRejectsUnknownSortField() {

        assertThrows(
                InvalidRequestException.class,
                () -> studentService.searchStudents(
                        null, null, null, null, 0, 10, "password", "asc"));

        verifyNoInteractions(repository);
    }

    @Test
    void testSearchRejectsOversizedPage() {

        assertThrows(
                InvalidRequestException.class,
                () -> studentService.searchStudents(
                        null, null, null, null, 0, 101, "id", "asc"));

        verifyNoInteractions(repository);
    }

    @Test
    void testSearchReturnsPageMetadataAndSortsWithIdTieBreaker() {

        Student student = new Student(
                1L, "Suresh", "suresh@gmail.com", "MCA", BigDecimal.valueOf(50000.0));

        StudentDTO dto = new StudentDTO(
                1L, "Suresh", "suresh@gmail.com", "MCA", BigDecimal.valueOf(50000.0));

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(0, 10), 1));

        when(modelMapper.map(student, StudentDTO.class)).thenReturn(dto);

        PageResponse<StudentDTO> result = studentService.searchStudents(
                "sur", "MCA", BigDecimal.valueOf(1000), null, 0, 10, "fee", "DESC");

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
        assertEquals(0, result.page());
        assertEquals(10, result.size());

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(repository).findAll(any(Specification.class), captor.capture());

        Pageable used = captor.getValue();

        assertEquals(
                org.springframework.data.domain.Sort.Direction.DESC,
                used.getSort().getOrderFor("fee").getDirection());

        assertNotNull(used.getSort().getOrderFor("id"));
    }
}
