package com.groupe.gestin_inscription.repository;

import com.groupe.gestin_inscription.model.Application;
import com.groupe.gestin_inscription.model.User;
import com.groupe.gestin_inscription.model.Enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    // Custom query methods can be added here if needed,
    /**
     * Finds all applications with a specific status.
     *
     * @param status The status of the applications to find.
     * @return A list of applications matching the status.
     */
    List<Application> findByStatus(ApplicationStatus status);

    /**
     * Finds all applications assigned to a specific administrator.
     * @param assignedAdminId The ID of the administrator.
     * @return A list of applications assigned to the administrator.
     */
    List<Application> findByAssignedAdmin_Id(Long assignedAdminId);

    @Query("SELECT a FROM Application a WHERE a.status = 'BLOCKED' AND a.lastUpdated < :blockedDateTime")
    List<Application> findBlockedApplications(@Param("blockedDateTime") LocalDateTime blockedDateTime);

    /**
     * Counts applications grouped by their status.
     * @return Map with status as key and count as value
     */
    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    Map<String, Integer> countApplicationsByStatus();

    List<Application> findByCompletionRateGreaterThanEqual(double rate);
    
    /**
     * Finds applications by applicant (User).
     * @param applicant The User who submitted the application.
     * @return A list of applications for the given applicant.
     */
    List<Application> findByApplicantName(User applicant);
    
    /**
     * Checks if an application exists for the given applicant.
     * @param applicant The User to check.
     * @return true if application exists, false otherwise.
     */
    boolean existsByApplicantName(User applicant);
    
    /**
     * Checks if an application exists for the given applicant, target institution and specialization.
     * @param applicant The User to check.
     * @param targetInstitution The target institution.
     * @param specialization The specialization.
     * @return true if application exists for this user, institution and specialization, false otherwise.
     */
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.applicantName = :applicant AND a.targetInstitution = :targetInstitution AND a.specialization = :specialization")
    boolean existsApplicationForUserAndInstitution(@Param("applicant") User applicant, @Param("targetInstitution") String targetInstitution, @Param("specialization") String specialization);
    
    /**
     * Counts applications by status.
     * @param status The application status.
     * @return Count of applications with the given status.
     */
    long countByStatus(ApplicationStatus status);
    
    /**
     * Simple query to get applications with user info without circular references
     * @return List of Object arrays containing application and user data
     * @throws org.springframework.dao.DataAccessException if database access fails
     */
    @Query("SELECT a.id, a.status, a.submissionDate, a.completionRate, u.firstName, u.lastName, u.email " +
           "FROM Application a LEFT JOIN a.applicantName u")
    List<Object[]> findApplicationsWithUserInfo();
    

}
