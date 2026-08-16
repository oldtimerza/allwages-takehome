package com.allwage.clockin.service;

import com.allwage.clockin.model.Employee;
import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.repository.employee.EmployeeRepository;
import com.allwage.clockin.repository.site.SiteDocumentStoreRepository;
import com.allwage.clockin.repository.site.SiteRepository;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SiteServiceConcurrencyTest {

    private static final String SITE_ID = "site-1";
    private static final String TEAM_ID = "team-1";
    private static final String EMPLOYEE_ID = "employee-1";
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, 1, 1);

    @Test
    void keepsEmployeeValidationAndAssignmentInOneDocumentStoreOperation() throws Exception {
        // Given
        DocumentStore store = new DocumentStore();
        SiteRepository siteRepository = new SiteDocumentStoreRepository(store);
        EmployeeRepository employeeRepository = new BlockingEmployeeRepository(store);
        siteRepository.save(new Site(SITE_ID, "Farm Alpha", null, List.of(new Team(TEAM_ID, "Harvest", null)), List.of(), List.of()));
        employeeRepository.save(new Employee(EMPLOYEE_ID, "Nomsa Dlamini", "+27115550123"));
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        BlockingEmployeeRepository blockingRepository = (BlockingEmployeeRepository) employeeRepository;
        AtomicBoolean assignmentExistedWhenEmployeeWasDeleted = new AtomicBoolean();
        CountDownLatch deletionStarted = new CountDownLatch(1);
        CountDownLatch deletionCompleted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // When
            Future<Optional<SiteAssignment>> assignment = executor.submit(
                () -> siteService.assignEmployee(SITE_ID, TEAM_ID, EMPLOYEE_ID, EFFECTIVE_FROM, null)
            );
            assertThat(blockingRepository.awaitEmployeeLookup()).isTrue();
            Future<Boolean> deletion = executor.submit(() -> {
                deletionStarted.countDown();
                try {
                    assignmentExistedWhenEmployeeWasDeleted.set(
                        siteRepository.findById(SITE_ID).orElseThrow().assignmentFor(EMPLOYEE_ID, EFFECTIVE_FROM).isPresent()
                    );
                    return store.delete("employees", EMPLOYEE_ID);
                } finally {
                    deletionCompleted.countDown();
                }
            });
            assertThat(deletionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(deletionCompleted.await(100, TimeUnit.MILLISECONDS)).isFalse();
            blockingRepository.allowEmployeeLookupToComplete();

            // Then
            assertThat(assignment.get()).contains(new SiteAssignment(EMPLOYEE_ID, TEAM_ID, EFFECTIVE_FROM, null, null));
            assertThat(deletion.get()).isTrue();
            assertThat(assignmentExistedWhenEmployeeWasDeleted).isTrue();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static final class BlockingEmployeeRepository implements EmployeeRepository {

        private final DocumentStore store;
        private final CountDownLatch employeeLookupStarted = new CountDownLatch(1);
        private final CountDownLatch allowEmployeeLookupToComplete = new CountDownLatch(1);

        private BlockingEmployeeRepository(DocumentStore store) {
            this.store = store;
        }

        @Override
        public void save(Employee employee) {
            store.save("employees", employee.id(), employee);
        }

        @Override
        public boolean saveIfAbsent(Employee employee) {
            try {
                store.saveIfAbsent("employees", employee.id(), employee);
                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        }

        @Override
        public Optional<Employee> findById(String id) {
            Optional<Employee> employee = store.findById("employees", id, Employee.class);
            employeeLookupStarted.countDown();
            try {
                allowEmployeeLookupToComplete.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Employee lookup was interrupted", exception);
            }
            return employee;
        }

        private boolean awaitEmployeeLookup() throws InterruptedException {
            return employeeLookupStarted.await(5, TimeUnit.SECONDS);
        }

        private void allowEmployeeLookupToComplete() {
            allowEmployeeLookupToComplete.countDown();
        }
    }
}
