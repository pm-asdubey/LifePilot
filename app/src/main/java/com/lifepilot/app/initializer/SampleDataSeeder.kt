package com.lifepilot.app.initializer

import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderPriority
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.model.VerificationStatus
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds realistic demo data so the app is populated on first launch.
 * Runs only once — skipped if objects already exist.
 */
@Singleton
class SampleDataSeeder @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val domainRepository: DomainRepository,
    private val reminderRepository: ReminderRepository,
) {

    suspend fun seedIfEmpty(profileId: String) {
        val existing = objectRepository.observeObjectsByProfile(profileId).firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return
        Timber.d("SampleDataSeeder: no objects found, seeding demo data")
        try {
            seedIdentity(profileId)
            seedCareer(profileId)
            seedFinance(profileId)
            seedHealth(profileId)
            seedTravel(profileId)
            seedDomainLifeStates(profileId)
            Timber.d("SampleDataSeeder: seeding complete")
        } catch (e: Exception) {
            Timber.e(e, "SampleDataSeeder: seeding failed")
        }
    }

    // ── Identity domain ───────────────────────────────────────────────────────

    private suspend fun seedIdentity(profileId: String) {
        // Passport
        val passport = objectRepository.createObject(
            profileId = profileId,
            objectType = "Passport",
            domain = "Identity",
            title = "Indian Passport",
            description = "Primary travel document",
        )
        meta(passport.objectId, "full_name", "Ashutosh Dubey", verified = true)
        meta(passport.objectId, "nationality", "Indian", verified = true)
        meta(passport.objectId, "passport_number", "Z4921835", verified = true)
        meta(passport.objectId, "date_of_birth", "1995-07-12", verified = true)
        meta(passport.objectId, "issue_date", "2018-03-14", verified = true)
        meta(passport.objectId, "expiry_date", "2028-03-13", verified = true)
        meta(passport.objectId, "place_of_issue", "Mumbai, India")
        meta(passport.objectId, "mrz", "P<INDUBEY<<ASHUTOSH<<<<<<<<<<<<<<<<<<<<<<<<")

        // Reminder: passport renewal in ~2 years
        reminder(
            objectId = passport.objectId,
            title = "Passport renewal due",
            message = "Your passport expires on 13 Mar 2028 — renew 6 months before international travel.",
            triggerDate = LocalDate.of(2027, 9, 13).toInstant(),
            priority = ReminderPriority.HIGH,
        )

        // Aadhaar
        val aadhaar = objectRepository.createObject(
            profileId = profileId,
            objectType = "Aadhaar",
            domain = "Identity",
            title = "Aadhaar Card",
            description = "National identity number",
        )
        meta(aadhaar.objectId, "aadhaar_number", "XXXX XXXX 3847", verified = true)
        meta(aadhaar.objectId, "registered_name", "Ashutosh Dubey", verified = true)
        meta(aadhaar.objectId, "registered_mobile", "+91-98XXXXXX72")
        meta(aadhaar.objectId, "address", "Flat 4B, Skyline Residency, Bengaluru, Karnataka 560034")

        // PAN Card
        val pan = objectRepository.createObject(
            profileId = profileId,
            objectType = "PAN Card",
            domain = "Identity",
            title = "PAN Card",
            description = "Income tax permanent account number",
        )
        meta(pan.objectId, "pan_number", "ABCPD1234E", verified = true)
        meta(pan.objectId, "name_on_card", "ASHUTOSH DUBEY", verified = true)
        meta(pan.objectId, "date_of_birth", "12/07/1995", verified = true)

        // Driving Licence
        val dl = objectRepository.createObject(
            profileId = profileId,
            objectType = "Driving Licence",
            domain = "Identity",
            title = "Driving Licence — Karnataka",
            description = "Class B non-transport motor vehicle",
        )
        meta(dl.objectId, "licence_number", "KA01-20190042371", verified = true)
        meta(dl.objectId, "full_name", "Ashutosh Dubey", verified = true)
        meta(dl.objectId, "vehicle_class", "LMV (Light Motor Vehicle)", verified = true)
        meta(dl.objectId, "issue_date", "2019-06-22", verified = true)
        meta(dl.objectId, "expiry_date", "2039-07-11", verified = true)
        meta(dl.objectId, "issuing_rto", "RTO Bengaluru East (KA-01)")
    }

    // ── Career domain ─────────────────────────────────────────────────────────

    private suspend fun seedCareer(profileId: String) {
        // Current job
        val job = objectRepository.createObject(
            profileId = profileId,
            objectType = "Job",
            domain = "Career",
            title = "Senior Associate, Accenture",
            description = "Product & technology consulting, digital transformation projects",
        )
        meta(job.objectId, "employer", "Accenture Technology Solutions", verified = true)
        meta(job.objectId, "job_title", "Senior Associate — Product & Technology", verified = true)
        meta(job.objectId, "employment_type", "Full-time, Permanent")
        meta(job.objectId, "start_date", "2021-08-02", verified = true)
        meta(job.objectId, "location", "Bengaluru, Karnataka (Hybrid)")
        meta(job.objectId, "department", "Technology Consulting")
        meta(job.objectId, "notice_period", "60 days")
        meta(job.objectId, "annual_ctc", "₹18,00,000 per annum")
        meta(job.objectId, "reporting_to", "Practice Lead")

        // Resume
        val resume = objectRepository.createObject(
            profileId = profileId,
            objectType = "Resume",
            domain = "Career",
            title = "Product Manager Resume — 2024",
            description = "Latest CV targeting PM and technology strategy roles",
        )
        meta(resume.objectId, "version", "2024-v3")
        meta(resume.objectId, "target_role", "Product Manager / Technology Strategy")
        meta(resume.objectId, "last_updated", "2024-11-18")
        meta(resume.objectId, "total_experience_years", "4")
        meta(resume.objectId, "key_skills", "Product Strategy, Agile, AI Systems, Stakeholder Management, Data Analysis")
        meta(resume.objectId, "education_highlight", "B.Tech Computer Science, IIIT Delhi (2013–2017)")
    }

    // ── Finance domain ────────────────────────────────────────────────────────

    private suspend fun seedFinance(profileId: String) {
        // HDFC Savings
        val savings = objectRepository.createObject(
            profileId = profileId,
            objectType = "Bank Account",
            domain = "Finance",
            title = "HDFC Savings Account",
            description = "Primary salary account",
        )
        meta(savings.objectId, "bank_name", "HDFC Bank", verified = true)
        meta(savings.objectId, "account_type", "Savings Account", verified = true)
        meta(savings.objectId, "account_number", "XXXX XXXX 3791", verified = true)
        meta(savings.objectId, "ifsc_code", "HDFC0004872")
        meta(savings.objectId, "branch", "Indiranagar, Bengaluru")
        meta(savings.objectId, "nominee", "Mother")
        meta(savings.objectId, "account_holder", "Ashutosh Dubey")

        // Health Insurance
        val insurance = objectRepository.createObject(
            profileId = profileId,
            objectType = "Insurance",
            domain = "Finance",
            title = "Star Health Individual Insurance",
            description = "₹5L individual health cover",
        )
        meta(insurance.objectId, "insurer", "Star Health & Allied Insurance", verified = true)
        meta(insurance.objectId, "policy_number", "P/211249/01/2024/001872", verified = true)
        meta(insurance.objectId, "sum_insured", "₹5,00,000")
        meta(insurance.objectId, "premium_annual", "₹8,420")
        meta(insurance.objectId, "policy_start", "2024-02-01", verified = true)
        meta(insurance.objectId, "policy_end", "2025-01-31")
        meta(insurance.objectId, "renewal_mode", "Annual")

        reminder(
            objectId = insurance.objectId,
            title = "Health insurance renewal due",
            message = "Policy expires 31 Jan 2025 — renew by 28 Jan to avoid lapse.",
            triggerDate = LocalDate.of(2025, 1, 20).toInstant(),
            priority = ReminderPriority.CRITICAL,
        )
    }

    // ── Health domain ─────────────────────────────────────────────────────────

    private suspend fun seedHealth(profileId: String) {
        val health = objectRepository.createObject(
            profileId = profileId,
            objectType = "Health Record",
            domain = "Health",
            title = "Annual Health Summary",
            description = "Preventive care and key health indicators",
        )
        meta(health.objectId, "blood_group", "B+", verified = true)
        meta(health.objectId, "known_allergies", "None known")
        meta(health.objectId, "last_full_checkup", "2024-03-15")
        meta(health.objectId, "bmi_last_recorded", "23.4")
        meta(health.objectId, "vision", "Left: -1.25, Right: -1.00")
        meta(health.objectId, "current_medications", "None")
        meta(health.objectId, "emergency_contact", "Father — +91-94XXXXXX18")
    }

    // ── Travel domain ─────────────────────────────────────────────────────────

    private suspend fun seedTravel(profileId: String) {
        val trip = objectRepository.createObject(
            profileId = profileId,
            objectType = "Travel",
            domain = "Travel",
            title = "London Business Trip — Feb 2025",
            description = "Client delivery sprint, Canary Wharf office",
        )
        meta(trip.objectId, "destination", "London, United Kingdom")
        meta(trip.objectId, "purpose", "Client Engagement — Technology Consulting")
        meta(trip.objectId, "departure_date", "2025-02-03")
        meta(trip.objectId, "return_date", "2025-02-14")
        meta(trip.objectId, "visa_required", "Yes — UK Standard Visitor Visa")
        meta(trip.objectId, "visa_status", "Pending application")
        meta(trip.objectId, "accommodation", "Premier Inn Canary Wharf, 12 nights")
        meta(trip.objectId, "flight_out", "AI-131 BLR→LHR, 03 Feb 23:10")
        meta(trip.objectId, "flight_return", "AI-132 LHR→BLR, 14 Feb 21:30")
        meta(trip.objectId, "forex_arranged", "£300 exchanged via HDFC @ 107.4")

        reminder(
            objectId = trip.objectId,
            title = "UK visa application deadline",
            message = "Apply at least 3 weeks before 3 Feb — target 13 Jan.",
            triggerDate = LocalDate.of(2025, 1, 13).toInstant(),
            priority = ReminderPriority.HIGH,
        )
    }

    // ── Domain life states ────────────────────────────────────────────────────

    private suspend fun seedDomainLifeStates(profileId: String) {
        domainRepository.upsertDomainLifeState(
            DomainLifeState(
                profileId = profileId,
                domain = "Identity",
                currentSituation = "All core identity documents are in order. Passport is valid until March 2028. Aadhaar and PAN are verified. Driving licence is valid until 2039.",
                currentPriorities = listOf("Initiate UK visa application for February trip"),
                knownRisks = listOf("Health insurance renewal in Jan 2025 — mark as CRITICAL"),
                openQuestions = listOf("Is the current passport sufficient for the London trip?"),
                recommendations = listOf("Begin UK visa application by 13 January"),
                recentChanges = listOf("Identity documents seeded on setup"),
                lastUpdated = Instant.now(),
                version = 1,
            )
        )

        domainRepository.upsertDomainLifeState(
            DomainLifeState(
                profileId = profileId,
                domain = "Career",
                currentSituation = "Currently a Senior Associate at Accenture Technology, Bengaluru. 4 years of experience across product and technology consulting. Actively working on digital transformation projects. Resume targets PM and technology strategy roles.",
                currentPriorities = listOf("Deliver London client sprint (Feb 2025)", "Update resume with 2024 projects"),
                knownRisks = listOf("Notice period is 60 days — factor into any transition planning"),
                openQuestions = listOf("What is the next career milestone?"),
                recommendations = listOf("Keep resume current — add Accenture 2024 highlights"),
                recentChanges = listOf("Career profile seeded on setup"),
                lastUpdated = Instant.now(),
                version = 1,
            )
        )

        domainRepository.upsertDomainLifeState(
            DomainLifeState(
                profileId = profileId,
                domain = "Finance",
                currentSituation = "Primary salary account with HDFC Bank. Health insurance with Star Health — renewal due January 2025. No active loans or major liabilities recorded.",
                currentPriorities = listOf("Renew health insurance before 31 Jan 2025"),
                knownRisks = listOf("Health insurance lapses on 31 Jan if not renewed"),
                openQuestions = listOf("Is the ₹5L sum insured sufficient?"),
                recommendations = listOf("Increase health cover to ₹10L at renewal"),
                recentChanges = listOf("Finance profile seeded on setup"),
                lastUpdated = Instant.now(),
                version = 1,
            )
        )

        domainRepository.upsertDomainLifeState(
            DomainLifeState(
                profileId = profileId,
                domain = "Travel",
                currentSituation = "London business trip planned for 3–14 February 2025. UK visa application not yet submitted. Flights and accommodation are booked.",
                currentPriorities = listOf("Submit UK visa application by 13 January"),
                knownRisks = listOf("Visa not applied — window closes in early January"),
                openQuestions = listOf("Has forex been arranged for the full trip duration?"),
                recommendations = listOf("Apply for UK visa immediately", "Arrange remaining forex"),
                recentChanges = listOf("London trip details added on setup"),
                lastUpdated = Instant.now(),
                version = 1,
            )
        )

        domainRepository.upsertDomainLifeState(
            DomainLifeState(
                profileId = profileId,
                domain = "Health",
                currentSituation = "Blood group B+. No known allergies. Last full health checkup was March 2024. BMI 23.4 — within healthy range. Vision correction required.",
                currentPriorities = listOf("Schedule next annual checkup around March 2025"),
                knownRisks = listOf("No emergency contact on file for hospital admission"),
                openQuestions = emptyList(),
                recommendations = listOf("Schedule annual checkup for March 2025"),
                recentChanges = listOf("Health record seeded on setup"),
                lastUpdated = Instant.now(),
                version = 1,
            )
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun meta(
        objectId: String,
        fieldId: String,
        value: String,
        verified: Boolean = false,
    ) {
        metadataRepository.upsertMetadata(
            objectId = objectId,
            fieldId = fieldId,
            value = value,
            source = MetadataSource.USER,
            confidence = if (verified) 1.0f else 0.9f,
            verificationStatus = if (verified) VerificationStatus.VERIFIED else VerificationStatus.UNVERIFIED,
        )
    }

    private suspend fun reminder(
        objectId: String,
        title: String,
        message: String,
        triggerDate: Instant,
        priority: ReminderPriority,
    ) {
        reminderRepository.createReminder(
            Reminder(
                reminderId = UUID.randomUUID().toString(),
                objectId = objectId,
                reminderType = "EXPIRY",
                triggerDate = triggerDate,
                priority = priority,
                status = ReminderStatus.SCHEDULED,
                title = title,
                message = message,
            )
        )
    }

    private fun LocalDate.toInstant(): Instant =
        atStartOfDay(ZoneId.systemDefault()).toInstant()
}
