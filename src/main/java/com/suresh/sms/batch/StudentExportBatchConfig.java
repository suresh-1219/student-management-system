package com.suresh.sms.batch;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.suresh.sms.entity.Student;

/**
 * The "all students, as CSV" batch job (Drop G3). Chunk-oriented, like every
 * Spring Batch step: reads a page of {@link Student} rows at a time from the
 * database and writes each chunk straight to a local CSV file, rather than
 * loading every student into memory at once or building the file by hand in
 * a service method.
 *
 * <p>This config only produces the CSV locally - it has no idea about
 * object storage or {@code ReportExport} rows. {@link ReportExportJobListener}
 * is what uploads the finished file and records the outcome; keeping that
 * split means a later report (courses, enrollments, ...) only needs its own
 * reader/writer pair here, not its own upload/bookkeeping logic too.
 */
@Configuration
public class StudentExportBatchConfig {

    public static final String JOB_NAME = "studentExportJob";
    public static final String STEP_NAME = "studentExportStep";

    // Small enough to keep memory flat regardless of how many students
    // exist, large enough that 10k students is a few dozen round trips, not
    // thousands.
    private static final int CHUNK_SIZE = 200;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Bean
    public Job studentExportJob(Step studentExportStep, ReportExportJobListener listener) {

        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(listener)
                .start(studentExportStep)
                .build();
    }

    @Bean
    public Step studentExportStep(
            JpaPagingItemReader<Student> studentItemReader,
            FlatFileItemWriter<Student> studentCsvItemWriter) {

        return new StepBuilder(STEP_NAME, jobRepository)
                .<Student, Student>chunk(CHUNK_SIZE, transactionManager)
                .reader(studentItemReader)
                .writer(studentCsvItemWriter)
                .build();
    }

    /**
     * {@code @StepScope}: a fresh reader (with its own paging cursor) per
     * job execution, not one singleton shared across every export anyone
     * ever triggers - the async {@code JobLauncher} (see
     * {@code BatchConfig}) means two exports really can run at once.
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Student> studentItemReader() {

        JpaPagingItemReader<Student> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT s FROM Student s ORDER BY s.id");
        reader.setPageSize(CHUNK_SIZE);
        // Not restartable in any meaningful way for this job (a failed
        // export is simply re-triggered from scratch by the client, not
        // resumed mid-file), so there is no execution-context state worth
        // persisting.
        reader.setSaveState(false);

        return reader;
    }

    /**
     * {@code @StepScope}: the output path depends on {@code filePath}, a job
     * parameter {@code ReportExportService} sets per export, so this can't
     * be a plain singleton bean.
     */
    @Bean
    @StepScope
    public FlatFileItemWriter<Student> studentCsvItemWriter(
            @Value("#{jobParameters['filePath']}") String filePath) {

        BeanWrapperFieldExtractor<Student> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] { "id", "name", "email", "course", "fee", "createdAt" });

        DelimitedLineAggregator<Student> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<Student> writer = new FlatFileItemWriter<>();
        writer.setName("studentCsvItemWriter");
        writer.setResource(new FileSystemResource(filePath));
        writer.setLineAggregator(lineAggregator);
        writer.setHeaderCallback(w -> w.write("id,name,email,course,fee,createdAt"));
        // An export with zero students is still a valid (empty-of-rows,
        // header-only) CSV, not an error - don't delete the file we just
        // told the client to expect.
        writer.setShouldDeleteIfEmpty(false);

        return writer;
    }
}
