package com.suresh.sms.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Adds an async {@link JobLauncher}, alongside Spring Boot's own
 * auto-configured one, which runs a job on the calling thread - fine for a
 * startup-time job, wrong here: a {@code POST /reports/students/export}
 * request would otherwise block for however long the whole export takes
 * (and time out, for a large enough student table) instead of returning as
 * soon as the export is accepted.
 *
 * <p>This bean is deliberately NOT named/registered as {@code jobLauncher}:
 * Boot's own {@code BatchAutoConfiguration$SpringBootBatchConfiguration}
 * already registers a bean of that exact name, and it is not
 * {@code @ConditionalOnMissingBean} the way a plain override would need -
 * two beans both named {@code jobLauncher} is a hard
 * {@code BeanDefinitionOverrideException} at startup, not something Spring
 * resolves for you. Giving this one a different name and {@link Primary}
 * instead lets both beans exist; {@code @Primary} is what makes every
 * plain {@code @Autowired JobLauncher} (see {@code ReportExportService})
 * resolve to this one rather than Boot's synchronous default.
 *
 * <p>{@link SimpleAsyncTaskExecutor} (a new thread per job, not a bounded
 * pool) is deliberate: report exports are rare, ad hoc, ADMIN-triggered
 * requests, not a high-volume path that needs pooling/backpressure - see
 * {@code ReportExportService#requestStudentExport}.
 */
@Configuration
public class BatchConfig {

    @Bean
    @Primary
    public JobLauncher reportExportJobLauncher(JobRepository jobRepository) throws Exception {

        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("report-export-"));
        launcher.afterPropertiesSet();

        return launcher;
    }
}
