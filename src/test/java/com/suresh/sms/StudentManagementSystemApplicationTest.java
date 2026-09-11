package com.suresh.sms;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class StudentManagementSystemApplicationTest {

    @Test
    void testMainMethod() {

        try (MockedStatic<SpringApplication> mocked =
                mockStatic(SpringApplication.class)) {

            StudentManagementSystemApplication.main(new String[] {});

            mocked.verify(() ->
                SpringApplication.run(
                    StudentManagementSystemApplication.class,
                    new String[] {}
                )
            );
        }
    }
}