package com.suresh.sms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.suresh.sms.dto.CourseDTO;
import com.suresh.sms.dto.SyllabusDTO;
import com.suresh.sms.exception.CourseSyllabusNotFoundException;
import com.suresh.sms.exception.InvalidRequestException;
import com.suresh.sms.exception.StorageException;
import com.suresh.sms.service.CourseService;
import com.suresh.sms.support.AbstractIntegrationTest;

/**
 * {@code CourseController}'s syllabus endpoints, with {@code CourseService}
 * mocked - these are plain web-layer tests for routing, status codes and
 * response shape (as opposed to {@code StorageServiceIntegrationTest},
 * which proves the real S3 round trip).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CourseSyllabusControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService service;

    @Test
    void testUploadSyllabusSuccess() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "content".getBytes());

        CourseDTO updated = new CourseDTO(1L, "CS101", "Intro to CS", 30);
        updated.setSyllabusFilename("syllabus.pdf");
        updated.setSyllabusSizeBytes(7L);

        when(service.uploadSyllabus(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(multipart("/courses/1/syllabus").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.syllabusFilename").value("syllabus.pdf"))
                .andExpect(jsonPath("$.data.syllabusSizeBytes").value(7));
    }

    @Test
    void testUploadSyllabusRejectsInvalidFile() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "bad".getBytes());

        when(service.uploadSyllabus(eq(1L), any()))
                .thenThrow(new InvalidRequestException(
                        "Syllabus must be a PDF, Word or PowerPoint document"));

        mockMvc.perform(multipart("/courses/1/syllabus").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Syllabus must be a PDF, Word or PowerPoint document"));
    }

    @Test
    void testUploadSyllabusSurfacesStorageFailureAsBadGateway() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "content".getBytes());

        when(service.uploadSyllabus(eq(1L), any()))
                .thenThrow(new StorageException("object store unreachable", new RuntimeException()));

        mockMvc.perform(multipart("/courses/1/syllabus").file(file))
                .andExpect(status().isBadGateway());
    }

    @Test
    void testGetSyllabusSuccess() throws Exception {

        SyllabusDTO syllabus = new SyllabusDTO(
                "syllabus.pdf", 7L, "https://objectstore.local/presigned", 600);

        when(service.getSyllabusDownload(1L)).thenReturn(syllabus);

        mockMvc.perform(get("/courses/1/syllabus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.filename").value("syllabus.pdf"))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://objectstore.local/presigned"));
    }

    @Test
    void testGetSyllabusNotFound() throws Exception {

        when(service.getSyllabusDownload(1L))
                .thenThrow(new CourseSyllabusNotFoundException("Course 1 has no syllabus uploaded"));

        mockMvc.perform(get("/courses/1/syllabus"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteSyllabusSuccess() throws Exception {

        mockMvc.perform(delete("/courses/1/syllabus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testDeleteSyllabusNotFound() throws Exception {

        doThrow(new CourseSyllabusNotFoundException("Course 1 has no syllabus uploaded"))
                .when(service).deleteSyllabus(1L);

        mockMvc.perform(delete("/courses/1/syllabus"))
                .andExpect(status().isNotFound());
    }
}
