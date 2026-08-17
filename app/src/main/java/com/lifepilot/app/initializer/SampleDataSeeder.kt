package com.lifepilot.app.initializer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.lifepilot.app.BuildConfig
import com.lifepilot.data.repository.PreferenceManager
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
import com.lifepilot.domain.usecase.UploadDocumentUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds comprehensive demo data so the app is richly populated on first launch.
 * Also generates and attaches realistic placeholder document images to key objects.
 * Runs only once — skipped if objects already exist.
 * If BuildConfig.DEMO_API_KEY is set at build time, pre-configures the AI provider.
 */
@Singleton
class SampleDataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val domainRepository: DomainRepository,
    private val reminderRepository: ReminderRepository,
    private val preferenceManager: PreferenceManager,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
) {

    private data class DocSpec(
        val objectId: String,
        val fileName: String,
        val docLabel: String,
        val headerColor: Int,
        val fields: List<Pair<String, String>>,
    )

    private val docQueue = mutableListOf<DocSpec>()

    suspend fun seedIfEmpty(profileId: String) {
        // Always run on every launch — only sets key if none is configured yet.
        seedDemoApiKey()
        val existing = objectRepository.observeObjectsByProfile(profileId).firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return
        Timber.d("SampleDataSeeder: no objects found, seeding demo data")
        docQueue.clear()
        try {
            seedIdentity(profileId)
            seedCareer(profileId)
            seedFinance(profileId)
            seedHousing(profileId)
            seedHealth(profileId)
            seedTravel(profileId)
            seedFamily(profileId)
            seedEducation(profileId)
            seedLegal(profileId)
            seedVehicles(profileId)
            seedTax(profileId)
            seedDomainLifeStates(profileId)
            seedDocuments()
            Timber.d("SampleDataSeeder: seeding complete")
        } catch (e: Exception) {
            Timber.e(e, "SampleDataSeeder: seeding failed")
        }
    }

    // ── Identity domain ───────────────────────────────────────────────────────

    private suspend fun seedIdentity(profileId: String) {
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
        meta(passport.objectId, "place_of_issue", "Mumbai, India", verified = true)
        meta(passport.objectId, "mrz", "P<INDUBEY<<ASHUTOSH<<<<<<<<<<<<<<<<<<<<<<<<")
        reminder(
            objectId = passport.objectId,
            title = "Passport renewal due",
            message = "Passport expires 13 Mar 2028 — renew at least 6 months before any international travel.",
            triggerDate = LocalDate.of(2027, 9, 13).toInstant(),
            priority = ReminderPriority.HIGH,
        )
        docQueue.add(DocSpec(
            objectId = passport.objectId,
            fileName = "Indian_Passport.jpg",
            docLabel = "Passport Scan",
            headerColor = Color.parseColor("#1A237E"),
            fields = listOf(
                "Passport Number" to "Z4921835",
                "Full Name" to "Ashutosh Dubey",
                "Nationality" to "Indian",
                "Date of Birth" to "12 Jul 1995",
                "Date of Issue" to "14 Mar 2018",
                "Date of Expiry" to "13 Mar 2028",
                "Place of Issue" to "Mumbai, India",
            ),
        ))

        val aadhaar = objectRepository.createObject(
            profileId = profileId,
            objectType = "Aadhaar",
            domain = "Identity",
            title = "Aadhaar Card",
            description = "National identity number",
        )
        meta(aadhaar.objectId, "aadhaar_number", "XXXX XXXX 3847", verified = true)
        meta(aadhaar.objectId, "registered_name", "Ashutosh Dubey", verified = true)
        meta(aadhaar.objectId, "registered_mobile", "+91-98XXXXXX72", verified = true)
        meta(aadhaar.objectId, "address", "Flat 4B, Skyline Residency, Bengaluru, Karnataka 560034")
        meta(aadhaar.objectId, "linked_pan", "ABCPD1234E")
        docQueue.add(DocSpec(
            objectId = aadhaar.objectId,
            fileName = "Aadhaar_Card.jpg",
            docLabel = "Aadhaar Card Scan",
            headerColor = Color.parseColor("#1565C0"),
            fields = listOf(
                "Aadhaar Number" to "XXXX XXXX 3847",
                "Registered Name" to "Ashutosh Dubey",
                "Date of Birth" to "12/07/1995",
                "Gender" to "Male",
                "Address" to "Flat 4B, Skyline Residency,\nBengaluru, Karnataka 560034",
            ),
        ))

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
        meta(pan.objectId, "father_name", "Rajesh Dubey")

        val dl = objectRepository.createObject(
            profileId = profileId,
            objectType = "Driving Licence",
            domain = "Identity",
            title = "Driving Licence — Karnataka",
            description = "Class LMV non-transport motor vehicle",
        )
        meta(dl.objectId, "licence_number", "KA01-20190042371", verified = true)
        meta(dl.objectId, "full_name", "Ashutosh Dubey", verified = true)
        meta(dl.objectId, "vehicle_class", "LMV (Light Motor Vehicle)", verified = true)
        meta(dl.objectId, "issue_date", "2019-06-22", verified = true)
        meta(dl.objectId, "expiry_date", "2039-07-11", verified = true)
        meta(dl.objectId, "issuing_rto", "RTO Bengaluru East (KA-01)")

        val voter = objectRepository.createObject(
            profileId = profileId,
            objectType = "Voter ID",
            domain = "Identity",
            title = "Voter ID Card",
            description = "Electoral photo identity card",
        )
        meta(voter.objectId, "epic_number", "KA/11/237/XXXXXX", verified = true)
        meta(voter.objectId, "full_name", "Ashutosh Dubey", verified = true)
        meta(voter.objectId, "constituency", "Bengaluru Central, Karnataka")
        meta(voter.objectId, "issue_date", "2016-10-05")
    }

    // ── Career domain ─────────────────────────────────────────────────────────

    private suspend fun seedCareer(profileId: String) {
        val currentJob = objectRepository.createObject(
            profileId = profileId,
            objectType = "Job",
            domain = "Career",
            title = "Senior Associate, Accenture",
            description = "Product & technology consulting, digital transformation projects",
        )
        meta(currentJob.objectId, "employer", "Accenture Technology Solutions", verified = true)
        meta(currentJob.objectId, "job_title", "Senior Associate — Product & Technology", verified = true)
        meta(currentJob.objectId, "employment_type", "Full-time, Permanent")
        meta(currentJob.objectId, "start_date", "2021-08-02", verified = true)
        meta(currentJob.objectId, "location", "Bengaluru, Karnataka (Hybrid)")
        meta(currentJob.objectId, "department", "Technology Consulting")
        meta(currentJob.objectId, "notice_period", "60 days")
        meta(currentJob.objectId, "annual_ctc", "₹18,00,000 per annum")
        meta(currentJob.objectId, "employee_id", "3109847")
        meta(currentJob.objectId, "pf_uan", "KABN00123456789")
        meta(currentJob.objectId, "reporting_to", "Practice Lead")

        val prevJob = objectRepository.createObject(
            profileId = profileId,
            objectType = "Job",
            domain = "Career",
            title = "Associate Consultant, Infosys BPM",
            description = "Process automation and analytics for banking clients",
        )
        meta(prevJob.objectId, "employer", "Infosys BPM Ltd", verified = true)
        meta(prevJob.objectId, "job_title", "Associate Consultant — Analytics")
        meta(prevJob.objectId, "start_date", "2019-07-15", verified = true)
        meta(prevJob.objectId, "end_date", "2021-07-30", verified = true)
        meta(prevJob.objectId, "location", "Pune, Maharashtra")
        meta(prevJob.objectId, "status", "INACTIVE")
        meta(prevJob.objectId, "annual_ctc", "₹9,50,000 per annum")
        meta(prevJob.objectId, "experience_letter_received", "Yes")

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
        meta(resume.objectId, "total_experience_years", "5.5")
        meta(resume.objectId, "key_skills", "Product Strategy, Agile, AI Systems, Stakeholder Management, Data Analysis")
        meta(resume.objectId, "education_highlight", "B.Tech Computer Science, IIIT Delhi (2013–2017)")
        docQueue.add(DocSpec(
            objectId = resume.objectId,
            fileName = "Product_Manager_Resume_2024.jpg",
            docLabel = "Resume — 2024",
            headerColor = Color.parseColor("#4A148C"),
            fields = listOf(
                "Name" to "Ashutosh Dubey",
                "Target Role" to "Product Manager / Technology Strategy",
                "Total Experience" to "5.5 Years",
                "Current Employer" to "Accenture Technology Solutions",
                "Education" to "B.Tech CS, IIIT Delhi (2017)",
                "Key Skills" to "Product Strategy · Agile · AI Systems · Data Analysis",
                "Certifications" to "AWS Solutions Architect · PMP",
            ),
        ))
    }

    // ── Finance domain ────────────────────────────────────────────────────────

    private suspend fun seedFinance(profileId: String) {
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
        meta(savings.objectId, "nominee", "Mother — Sunita Dubey")
        meta(savings.objectId, "account_holder", "Ashutosh Dubey")
        meta(savings.objectId, "net_banking_active", "Yes")

        val sbiAccount = objectRepository.createObject(
            profileId = profileId,
            objectType = "Bank Account",
            domain = "Finance",
            title = "SBI Savings Account",
            description = "Secondary account for EMI and recurring deposits",
        )
        meta(sbiAccount.objectId, "bank_name", "State Bank of India", verified = true)
        meta(sbiAccount.objectId, "account_type", "Savings Account")
        meta(sbiAccount.objectId, "account_number", "XXXX XXXX 5512", verified = true)
        meta(sbiAccount.objectId, "ifsc_code", "SBIN0003728")
        meta(sbiAccount.objectId, "branch", "Bilaspur, Chhattisgarh")
        meta(sbiAccount.objectId, "nominee", "Father — Rajesh Dubey")

        val healthIns = objectRepository.createObject(
            profileId = profileId,
            objectType = "Insurance",
            domain = "Finance",
            title = "Star Health Individual Insurance",
            description = "₹5L individual health cover",
        )
        meta(healthIns.objectId, "insurer", "Star Health & Allied Insurance", verified = true)
        meta(healthIns.objectId, "policy_number", "P/211249/01/2024/001872", verified = true)
        meta(healthIns.objectId, "policy_type", "Individual Health Insurance")
        meta(healthIns.objectId, "sum_insured", "₹5,00,000")
        meta(healthIns.objectId, "premium_annual", "₹8,420")
        meta(healthIns.objectId, "policy_start", "2024-02-01", verified = true)
        meta(healthIns.objectId, "policy_end", "2025-01-31")
        meta(healthIns.objectId, "renewal_mode", "Annual")
        meta(healthIns.objectId, "nominee", "Mother — Sunita Dubey")
        reminder(
            objectId = healthIns.objectId,
            title = "Health insurance renewal due",
            message = "Policy expires 31 Jan 2025 — renew by 28 Jan to avoid lapse.",
            triggerDate = LocalDate.of(2025, 1, 20).toInstant(),
            priority = ReminderPriority.CRITICAL,
        )
        docQueue.add(DocSpec(
            objectId = healthIns.objectId,
            fileName = "Star_Health_Policy.jpg",
            docLabel = "Insurance Policy Document",
            headerColor = Color.parseColor("#B71C1C"),
            fields = listOf(
                "Policy Number" to "P/211249/01/2024/001872",
                "Policy Holder" to "Ashutosh Dubey",
                "Insurer" to "Star Health & Allied Insurance",
                "Type" to "Individual Health Insurance",
                "Sum Insured" to "₹5,00,000",
                "Annual Premium" to "₹8,420",
                "Policy Period" to "01 Feb 2024 – 31 Jan 2025",
                "Nominee" to "Sunita Dubey (Mother)",
            ),
        ))

        val termIns = objectRepository.createObject(
            profileId = profileId,
            objectType = "Insurance",
            domain = "Finance",
            title = "LIC Term Plan — Jeevan Amar",
            description = "₹1 Cr pure term life cover",
        )
        meta(termIns.objectId, "insurer", "Life Insurance Corporation of India", verified = true)
        meta(termIns.objectId, "policy_number", "LIC/948271936", verified = true)
        meta(termIns.objectId, "policy_type", "Term Insurance")
        meta(termIns.objectId, "sum_assured", "₹1,00,00,000")
        meta(termIns.objectId, "premium_annual", "₹14,650")
        meta(termIns.objectId, "policy_start", "2022-06-01", verified = true)
        meta(termIns.objectId, "policy_end", "2052-06-01")
        meta(termIns.objectId, "nominee", "Mother — Sunita Dubey")
        meta(termIns.objectId, "premium_due_month", "June")
        docQueue.add(DocSpec(
            objectId = termIns.objectId,
            fileName = "LIC_Jeevan_Amar_Policy.jpg",
            docLabel = "LIC Term Policy Document",
            headerColor = Color.parseColor("#1B5E20"),
            fields = listOf(
                "Policy Number" to "LIC/948271936",
                "Policy Holder" to "Ashutosh Dubey",
                "Insurer" to "Life Insurance Corporation of India",
                "Plan" to "Jeevan Amar — Term Plan",
                "Sum Assured" to "₹1,00,00,000",
                "Annual Premium" to "₹14,650",
                "Policy Period" to "01 Jun 2022 – 01 Jun 2052",
                "Nominee" to "Sunita Dubey (Mother)",
            ),
        ))

        val mf = objectRepository.createObject(
            profileId = profileId,
            objectType = "Investment",
            domain = "Finance",
            title = "Zerodha Mutual Fund Portfolio",
            description = "SIP investments in equity and hybrid funds",
        )
        meta(mf.objectId, "platform", "Zerodha Coin", verified = true)
        meta(mf.objectId, "monthly_sip_amount", "₹10,000")
        meta(mf.objectId, "funds", "Mirae Asset Large Cap (₹4K), Axis Mid Cap (₹3K), HDFC Balanced Advantage (₹3K)")
        meta(mf.objectId, "total_invested_approx", "₹3,60,000")
        meta(mf.objectId, "current_value_approx", "₹4,12,000")
        meta(mf.objectId, "sip_start_date", "2022-01-10")
        meta(mf.objectId, "nominee", "Mother — Sunita Dubey")

        val epf = objectRepository.createObject(
            profileId = profileId,
            objectType = "Provident Fund",
            domain = "Finance",
            title = "EPF — Employee Provident Fund",
            description = "Mandatory retirement savings via employer",
        )
        meta(epf.objectId, "uan", "KABN00123456789", verified = true)
        meta(epf.objectId, "employer_contribution_monthly", "₹1,800 approx")
        meta(epf.objectId, "employee_contribution_monthly", "₹1,800 approx")
        meta(epf.objectId, "total_balance_approx", "₹2,80,000")
        meta(epf.objectId, "linked_employer", "Accenture Technology Solutions")
        meta(epf.objectId, "nominee", "Mother — Sunita Dubey")

        val creditCard = objectRepository.createObject(
            profileId = profileId,
            objectType = "Credit Card",
            domain = "Finance",
            title = "HDFC Regalia Credit Card",
            description = "Primary credit card for rewards and travel",
        )
        meta(creditCard.objectId, "issuer", "HDFC Bank", verified = true)
        meta(creditCard.objectId, "card_type", "Regalia — Rewards & Travel")
        meta(creditCard.objectId, "card_number_last4", "XXXX XXXX XXXX 8743", verified = true)
        meta(creditCard.objectId, "credit_limit", "₹4,00,000")
        meta(creditCard.objectId, "billing_cycle", "15th of each month")
        meta(creditCard.objectId, "payment_due_day", "5th of following month")
        meta(creditCard.objectId, "rewards_points", "18,420 points")
        reminder(
            objectId = creditCard.objectId,
            title = "Credit card payment due",
            message = "HDFC Regalia bill due on the 5th — pay in full to avoid interest.",
            triggerDate = LocalDate.now().withDayOfMonth(3).toInstant(),
            priority = ReminderPriority.HIGH,
        )
    }

    // ── Housing domain ────────────────────────────────────────────────────────

    private suspend fun seedHousing(profileId: String) {
        val flat = objectRepository.createObject(
            profileId = profileId,
            objectType = "Property",
            domain = "Housing",
            title = "Rental Flat — Indiranagar, Bengaluru",
            description = "Current residence, 2 BHK",
        )
        meta(flat.objectId, "address", "Flat 4B, Skyline Residency, 12th Main, Indiranagar, Bengaluru 560034", verified = true)
        meta(flat.objectId, "property_type", "2 BHK Residential Apartment")
        meta(flat.objectId, "status", "Rented")
        meta(flat.objectId, "monthly_rent", "₹28,000")
        meta(flat.objectId, "lease_start", "2022-09-01", verified = true)
        meta(flat.objectId, "lease_end", "2025-08-31")
        meta(flat.objectId, "security_deposit", "₹84,000")
        meta(flat.objectId, "landlord_name", "Mr. Suresh Patel")
        meta(flat.objectId, "landlord_mobile", "+91-98765XXXXX")
        meta(flat.objectId, "area_sqft", "1,050 sq ft")
        meta(flat.objectId, "furnishing", "Semi-furnished")
        reminder(
            objectId = flat.objectId,
            title = "Lease renewal due",
            message = "Lease expires 31 Aug 2025 — initiate renewal or relocation notice 2 months before.",
            triggerDate = LocalDate.of(2025, 6, 30).toInstant(),
            priority = ReminderPriority.HIGH,
        )
        docQueue.add(DocSpec(
            objectId = flat.objectId,
            fileName = "Rental_Agreement_Indiranagar.jpg",
            docLabel = "Rental Agreement",
            headerColor = Color.parseColor("#004D40"),
            fields = listOf(
                "Property" to "Flat 4B, Skyline Residency",
                "Address" to "12th Main, Indiranagar, Bengaluru 560034",
                "Tenant" to "Ashutosh Dubey",
                "Landlord" to "Mr. Suresh Patel",
                "Monthly Rent" to "₹28,000",
                "Security Deposit" to "₹84,000",
                "Lease Period" to "01 Sep 2022 – 31 Aug 2025",
                "Furnishing" to "Semi-furnished — 1,050 sq ft",
            ),
        ))

        val ancestralHome = objectRepository.createObject(
            profileId = profileId,
            objectType = "Property",
            domain = "Housing",
            title = "Ancestral Home — Bilaspur",
            description = "Family home in Bilaspur, Chhattisgarh",
        )
        meta(ancestralHome.objectId, "address", "House No. 42, Civil Lines, Bilaspur, Chhattisgarh 495001", verified = true)
        meta(ancestralHome.objectId, "property_type", "Independent House (3 BHK)")
        meta(ancestralHome.objectId, "status", "Owned — Family occupied")
        meta(ancestralHome.objectId, "ownership", "Parents — Rajesh & Sunita Dubey")
        meta(ancestralHome.objectId, "area_sqft", "1,800 sq ft")
        meta(ancestralHome.objectId, "land_area", "2,400 sq ft")
        meta(ancestralHome.objectId, "property_tax_due", "Paid for 2024-25")
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
        meta(health.objectId, "known_allergies", "None known", verified = true)
        meta(health.objectId, "last_full_checkup", "2024-03-15")
        meta(health.objectId, "bmi_last_recorded", "23.4 (healthy range)")
        meta(health.objectId, "height", "5 ft 10 in")
        meta(health.objectId, "weight", "74 kg")
        meta(health.objectId, "vision", "Left: -1.25, Right: -1.00 (corrected)")
        meta(health.objectId, "dental_last_visit", "2023-12-10")
        meta(health.objectId, "current_medications", "None")
        meta(health.objectId, "emergency_contact", "Father — Rajesh Dubey, +91-94XXXXXX18")
        reminder(
            objectId = health.objectId,
            title = "Annual health checkup due",
            message = "Last checkup was March 2024 — schedule next by March 2025.",
            triggerDate = LocalDate.of(2025, 3, 1).toInstant(),
            priority = ReminderPriority.MEDIUM,
        )
        docQueue.add(DocSpec(
            objectId = health.objectId,
            fileName = "Health_Checkup_Report_2024.jpg",
            docLabel = "Annual Health Report 2024",
            headerColor = Color.parseColor("#880E4F"),
            fields = listOf(
                "Patient Name" to "Ashutosh Dubey",
                "Date of Report" to "15 March 2024",
                "Blood Group" to "B+",
                "Height / Weight" to "5 ft 10 in / 74 kg",
                "BMI" to "23.4 (Normal Range)",
                "Vision" to "L: -1.25, R: -1.00",
                "Allergies" to "None Known",
                "Current Medications" to "None",
            ),
        ))

        val prescription = objectRepository.createObject(
            profileId = profileId,
            objectType = "Medical Record",
            domain = "Health",
            title = "Vision Prescription — 2024",
            description = "Eyeglasses / contact lens prescription",
        )
        meta(prescription.objectId, "prescribing_doctor", "Dr. Priya Nair, Sankara Nethralaya, Bengaluru")
        meta(prescription.objectId, "date", "2024-01-20")
        meta(prescription.objectId, "right_eye_sph", "-1.00")
        meta(prescription.objectId, "left_eye_sph", "-1.25")
        meta(prescription.objectId, "validity", "1 year (renew Jan 2025)")
        meta(prescription.objectId, "frames_brand", "Titan Eyeplus — Flexon frame")
        reminder(
            objectId = prescription.objectId,
            title = "Vision prescription renewal",
            message = "Prescription expires Jan 2025 — schedule eye exam.",
            triggerDate = LocalDate.of(2025, 1, 10).toInstant(),
            priority = ReminderPriority.LOW,
        )
    }

    // ── Travel domain ─────────────────────────────────────────────────────────

    private suspend fun seedTravel(profileId: String) {
        val londonTrip = objectRepository.createObject(
            profileId = profileId,
            objectType = "Travel",
            domain = "Travel",
            title = "London Business Trip — Feb 2025",
            description = "Client delivery sprint, Canary Wharf office",
        )
        meta(londonTrip.objectId, "destination", "London, United Kingdom", verified = true)
        meta(londonTrip.objectId, "purpose", "Client Engagement — Technology Consulting")
        meta(londonTrip.objectId, "departure_date", "2025-02-03", verified = true)
        meta(londonTrip.objectId, "return_date", "2025-02-14", verified = true)
        meta(londonTrip.objectId, "visa_required", "Yes — UK Standard Visitor Visa")
        meta(londonTrip.objectId, "visa_status", "Granted", verified = true)
        meta(londonTrip.objectId, "accommodation", "Premier Inn Canary Wharf, 12 nights")
        meta(londonTrip.objectId, "flight_out", "AI-131 BLR→LHR, 03 Feb 23:10")
        meta(londonTrip.objectId, "flight_return", "AI-132 LHR→BLR, 14 Feb 21:30")
        meta(londonTrip.objectId, "forex_arranged", "£400 exchanged via HDFC @ 107.4")
        meta(londonTrip.objectId, "travel_insurance", "HDFC ERGO Travel Guard — TRV/2025/00384")
        docQueue.add(DocSpec(
            objectId = londonTrip.objectId,
            fileName = "UK_Visa_Grant_2025.jpg",
            docLabel = "UK Visa Grant Letter",
            headerColor = Color.parseColor("#01579B"),
            fields = listOf(
                "Visa Type" to "UK Standard Visitor Visa",
                "Applicant" to "Ashutosh Dubey",
                "Passport Number" to "Z4921835",
                "Valid From" to "15 January 2025",
                "Valid Until" to "15 July 2025",
                "Duration" to "6 Months (Multiple Entry)",
                "Issued By" to "UK Visas & Immigration",
                "Reference" to "GWF059XXXXXXX",
            ),
        ))

        val dubaiTrip = objectRepository.createObject(
            profileId = profileId,
            objectType = "Travel",
            domain = "Travel",
            title = "Dubai Holiday — Dec 2023",
            description = "Personal vacation with friends",
        )
        meta(dubaiTrip.objectId, "destination", "Dubai, UAE", verified = true)
        meta(dubaiTrip.objectId, "purpose", "Personal Holiday")
        meta(dubaiTrip.objectId, "departure_date", "2023-12-20", verified = true)
        meta(dubaiTrip.objectId, "return_date", "2023-12-27", verified = true)
        meta(dubaiTrip.objectId, "visa_type", "UAE Visa on Arrival — Indian passport eligible")
        meta(dubaiTrip.objectId, "hotel", "Rove City Centre, Dubai")
        meta(dubaiTrip.objectId, "trip_status", "Completed")
    }

    // ── Family domain ─────────────────────────────────────────────────────────

    private suspend fun seedFamily(profileId: String) {
        val father = objectRepository.createObject(
            profileId = profileId,
            objectType = "Family Member",
            domain = "Family",
            title = "Father — Rajesh Dubey",
            description = "Father, retired government officer",
        )
        meta(father.objectId, "full_name", "Rajesh Kumar Dubey", verified = true)
        meta(father.objectId, "relationship", "Father")
        meta(father.objectId, "date_of_birth", "1965-03-22")
        meta(father.objectId, "mobile", "+91-94XXXXXX18")
        meta(father.objectId, "occupation", "Retired — State Government Service")
        meta(father.objectId, "location", "Bilaspur, Chhattisgarh")
        meta(father.objectId, "blood_group", "O+")

        val mother = objectRepository.createObject(
            profileId = profileId,
            objectType = "Family Member",
            domain = "Family",
            title = "Mother — Sunita Dubey",
            description = "Mother, homemaker",
        )
        meta(mother.objectId, "full_name", "Sunita Dubey", verified = true)
        meta(mother.objectId, "relationship", "Mother")
        meta(mother.objectId, "date_of_birth", "1968-11-07")
        meta(mother.objectId, "mobile", "+91-98XXXXXX34")
        meta(mother.objectId, "occupation", "Homemaker")
        meta(mother.objectId, "location", "Bilaspur, Chhattisgarh")
        meta(mother.objectId, "blood_group", "B+")
        meta(mother.objectId, "nominee_on", "HDFC savings, LIC term plan, Star Health, HDFC MF portfolio")
    }

    // ── Education domain ──────────────────────────────────────────────────────

    private suspend fun seedEducation(profileId: String) {
        val degree = objectRepository.createObject(
            profileId = profileId,
            objectType = "Education",
            domain = "Education",
            title = "B.Tech Computer Science — IIIT Delhi",
            description = "Undergraduate degree, specialisation in AI & Software Systems",
        )
        meta(degree.objectId, "institution", "Indraprastha Institute of Information Technology Delhi (IIIT-D)", verified = true)
        meta(degree.objectId, "degree", "Bachelor of Technology (B.Tech)", verified = true)
        meta(degree.objectId, "specialisation", "Computer Science & Engineering (AI & Software Systems)")
        meta(degree.objectId, "start_year", "2013")
        meta(degree.objectId, "graduation_year", "2017", verified = true)
        meta(degree.objectId, "cgpa", "8.2 / 10")
        meta(degree.objectId, "dissertation_title", "Adaptive Task Scheduling in Distributed Systems")
        docQueue.add(DocSpec(
            objectId = degree.objectId,
            fileName = "IIIT_Delhi_Degree_Certificate.jpg",
            docLabel = "Degree Certificate",
            headerColor = Color.parseColor("#1A237E"),
            fields = listOf(
                "Institution" to "IIIT Delhi (IIIT-D)",
                "Degree" to "Bachelor of Technology (B.Tech)",
                "Specialisation" to "Computer Science & Engineering",
                "Graduation Year" to "2017",
                "CGPA" to "8.2 / 10",
                "Roll Number" to "MT17XXXX",
                "Dissertation" to "Adaptive Task Scheduling in Distributed Systems",
            ),
        ))

        val awsCert = objectRepository.createObject(
            profileId = profileId,
            objectType = "Certification",
            domain = "Education",
            title = "AWS Certified Solutions Architect",
            description = "Amazon Web Services professional certification",
        )
        meta(awsCert.objectId, "issuing_body", "Amazon Web Services (AWS)", verified = true)
        meta(awsCert.objectId, "certification_name", "AWS Certified Solutions Architect — Associate")
        meta(awsCert.objectId, "certification_id", "AWS-ASA-2023-XXXX")
        meta(awsCert.objectId, "issue_date", "2023-05-18", verified = true)
        meta(awsCert.objectId, "expiry_date", "2026-05-18")
        meta(awsCert.objectId, "level", "Associate")
        reminder(
            objectId = awsCert.objectId,
            title = "AWS certification renewal",
            message = "AWS Solutions Architect cert expires May 2026 — renew or recertify 3 months before.",
            triggerDate = LocalDate.of(2026, 2, 18).toInstant(),
            priority = ReminderPriority.MEDIUM,
        )
        docQueue.add(DocSpec(
            objectId = awsCert.objectId,
            fileName = "AWS_Solutions_Architect_Certificate.jpg",
            docLabel = "AWS Certificate",
            headerColor = Color.parseColor("#E65100"),
            fields = listOf(
                "Certification" to "AWS Certified Solutions Architect",
                "Level" to "Associate",
                "Certificate Holder" to "Ashutosh Dubey",
                "Certificate ID" to "AWS-ASA-2023-XXXX",
                "Issued By" to "Amazon Web Services (AWS)",
                "Issue Date" to "18 May 2023",
                "Expiry Date" to "18 May 2026",
            ),
        ))

        val pmp = objectRepository.createObject(
            profileId = profileId,
            objectType = "Certification",
            domain = "Education",
            title = "PMP — Project Management Professional",
            description = "PMI certification for project management",
        )
        meta(pmp.objectId, "issuing_body", "Project Management Institute (PMI)", verified = true)
        meta(pmp.objectId, "certification_name", "Project Management Professional (PMP)")
        meta(pmp.objectId, "certification_id", "PMP-2249XXXX")
        meta(pmp.objectId, "issue_date", "2024-02-10", verified = true)
        meta(pmp.objectId, "expiry_date", "2027-02-10")
        meta(pmp.objectId, "pdu_required_to_renew", "60 PDUs in 3 years")
        docQueue.add(DocSpec(
            objectId = pmp.objectId,
            fileName = "PMP_Certificate.jpg",
            docLabel = "PMP Certificate",
            headerColor = Color.parseColor("#33691E"),
            fields = listOf(
                "Certification" to "Project Management Professional (PMP)",
                "Certificate Holder" to "Ashutosh Dubey",
                "Certificate ID" to "PMP-2249XXXX",
                "Issued By" to "Project Management Institute (PMI)",
                "Issue Date" to "10 February 2024",
                "Expiry Date" to "10 February 2027",
                "PDUs Required" to "60 PDUs in 3 years",
            ),
        ))
    }

    // ── Legal domain ──────────────────────────────────────────────────────────

    private suspend fun seedLegal(profileId: String) {
        val employmentContract = objectRepository.createObject(
            profileId = profileId,
            objectType = "Contract",
            domain = "Legal",
            title = "Accenture Employment Contract",
            description = "Permanent employment agreement with Accenture Technology Solutions",
        )
        meta(employmentContract.objectId, "parties", "Accenture Technology Solutions (Employer) & Ashutosh Dubey (Employee)", verified = true)
        meta(employmentContract.objectId, "contract_type", "Permanent Employment Agreement")
        meta(employmentContract.objectId, "effective_date", "2021-08-02", verified = true)
        meta(employmentContract.objectId, "governing_law", "Indian Contract Act, 1872")
        meta(employmentContract.objectId, "notice_period", "60 days")
        meta(employmentContract.objectId, "non_compete_clause", "12 months post-employment in direct competition")
        meta(employmentContract.objectId, "ip_assignment", "All work product during employment is assigned to Accenture")
        meta(employmentContract.objectId, "signed_by_employee", "Yes — 02 Aug 2021")
        meta(employmentContract.objectId, "signed_by_employer", "Yes — HR, Accenture")
        docQueue.add(DocSpec(
            objectId = employmentContract.objectId,
            fileName = "Accenture_Employment_Contract.jpg",
            docLabel = "Employment Contract",
            headerColor = Color.parseColor("#4E342E"),
            fields = listOf(
                "Contract Type" to "Permanent Employment Agreement",
                "Employer" to "Accenture Technology Solutions India Pvt. Ltd.",
                "Employee" to "Ashutosh Dubey",
                "Designation" to "Senior Associate — Product & Technology",
                "Effective Date" to "02 August 2021",
                "Notice Period" to "60 Days",
                "Governing Law" to "Indian Contract Act, 1872",
                "Non-Compete" to "12 months post-employment",
            ),
        ))

        val rentalAgreementLegal = objectRepository.createObject(
            profileId = profileId,
            objectType = "Agreement",
            domain = "Legal",
            title = "Tenancy Agreement — Indiranagar Flat",
            description = "Registered leave and licence agreement for current residence",
        )
        meta(rentalAgreementLegal.objectId, "agreement_type", "Leave and Licence Agreement")
        meta(rentalAgreementLegal.objectId, "licensor", "Mr. Suresh Patel", verified = true)
        meta(rentalAgreementLegal.objectId, "licensee", "Ashutosh Dubey", verified = true)
        meta(rentalAgreementLegal.objectId, "property", "Flat 4B, Skyline Residency, 12th Main, Indiranagar, Bengaluru 560034", verified = true)
        meta(rentalAgreementLegal.objectId, "duration", "36 months (Sep 2022 – Aug 2025)")
        meta(rentalAgreementLegal.objectId, "stamp_duty_paid", "Yes — Karnataka Stamp Act")
        meta(rentalAgreementLegal.objectId, "registration_number", "KA/REG/2022/BLR/XXXXXX")
        meta(rentalAgreementLegal.objectId, "notarised", "Yes — Notary Public, Bengaluru")
        meta(rentalAgreementLegal.objectId, "monthly_licence_fee", "₹28,000")
        meta(rentalAgreementLegal.objectId, "deposit_amount", "₹84,000")

        val nda = objectRepository.createObject(
            profileId = profileId,
            objectType = "Contract",
            domain = "Legal",
            title = "NDA — London Client Engagement",
            description = "Mutual NDA for the UK-based client project",
        )
        meta(nda.objectId, "agreement_type", "Mutual Non-Disclosure Agreement")
        meta(nda.objectId, "parties", "Accenture Technology Solutions & [Client] UK Ltd", verified = true)
        meta(nda.objectId, "effective_date", "2025-01-15")
        meta(nda.objectId, "duration", "3 years from effective date")
        meta(nda.objectId, "governing_law", "England and Wales")
        meta(nda.objectId, "signed", "Yes — by both parties")
        meta(nda.objectId, "applicable_to_employee", "Ashutosh Dubey — named in Schedule A")

        val willPoa = objectRepository.createObject(
            profileId = profileId,
            objectType = "Legal Document",
            domain = "Legal",
            title = "Power of Attorney — Father",
            description = "General PoA granted to father Rajesh Dubey for Bilaspur property matters",
        )
        meta(willPoa.objectId, "document_type", "General Power of Attorney")
        meta(willPoa.objectId, "grantor", "Ashutosh Dubey")
        meta(willPoa.objectId, "grantee", "Rajesh Kumar Dubey (Father)")
        meta(willPoa.objectId, "scope", "Bilaspur ancestral property — registration, sale, leasing, litigation")
        meta(willPoa.objectId, "notarised_date", "2023-05-10")
        meta(willPoa.objectId, "notary_office", "Notary Public, Bilaspur District Court")
        meta(willPoa.objectId, "validity", "Indefinite until revoked in writing")
        meta(willPoa.objectId, "registered", "Yes — Sub-Registrar Office, Bilaspur")
        docQueue.add(DocSpec(
            objectId = willPoa.objectId,
            fileName = "Power_of_Attorney_Bilaspur.jpg",
            docLabel = "Power of Attorney",
            headerColor = Color.parseColor("#37474F"),
            fields = listOf(
                "Document Type" to "General Power of Attorney",
                "Grantor" to "Ashutosh Dubey (S/o Rajesh Kumar Dubey)",
                "Attorney / Grantee" to "Rajesh Kumar Dubey (Father)",
                "Scope" to "Bilaspur ancestral property — all dealings",
                "Notarised" to "10 May 2023, Bilaspur District Court",
                "Registered" to "Sub-Registrar Office, Bilaspur",
                "Validity" to "Indefinite until revoked in writing",
            ),
        ))
    }

    // ── Vehicles domain ───────────────────────────────────────────────────────

    private suspend fun seedVehicles(profileId: String) {
        val bike = objectRepository.createObject(
            profileId = profileId,
            objectType = "Vehicle",
            domain = "Vehicles",
            title = "Honda CB Hornet 160R — KA01 MJ 5832",
            description = "Personal commuter motorcycle",
        )
        meta(bike.objectId, "vehicle_type", "Motorcycle — 160cc")
        meta(bike.objectId, "make_model", "Honda CB Hornet 160R", verified = true)
        meta(bike.objectId, "registration_number", "KA01 MJ 5832", verified = true)
        meta(bike.objectId, "color", "Athletic Blue Metallic")
        meta(bike.objectId, "year_of_manufacture", "2019")
        meta(bike.objectId, "engine_number", "JC74EXXXXXXXX")
        meta(bike.objectId, "chassis_number", "ME4JC74BXKXXXXXXXX")
        meta(bike.objectId, "rc_issue_date", "2019-10-28")
        meta(bike.objectId, "hypothecation", "Nil — self-funded")
        meta(bike.objectId, "fuel_type", "Petrol")
        meta(bike.objectId, "insurance_provider", "Bajaj Allianz — OD + TP")
        meta(bike.objectId, "insurance_policy_number", "OG-24-1801-1802-XXXXXXX")
        meta(bike.objectId, "insurance_expiry", "2025-10-27")
        meta(bike.objectId, "puc_expiry", "2025-02-15")
        meta(bike.objectId, "fitness_certificate_expiry", "2034-10-27")
        meta(bike.objectId, "odometer_km", "22,400 km")
        reminder(
            objectId = bike.objectId,
            title = "Bike insurance renewal due",
            message = "Bajaj Allianz bike insurance expires 27 Oct 2025 — renew before lapse.",
            triggerDate = LocalDate.of(2025, 10, 15).toInstant(),
            priority = ReminderPriority.HIGH,
        )
        reminder(
            objectId = bike.objectId,
            title = "PUC certificate renewal due",
            message = "Pollution Under Control certificate expires 15 Feb 2025 — renew at nearest PUC centre.",
            triggerDate = LocalDate.of(2025, 2, 10).toInstant(),
            priority = ReminderPriority.MEDIUM,
        )
        docQueue.add(DocSpec(
            objectId = bike.objectId,
            fileName = "Honda_CB_Hornet_RC.jpg",
            docLabel = "Vehicle Registration Certificate",
            headerColor = Color.parseColor("#1B5E20"),
            fields = listOf(
                "Registration Number" to "KA01 MJ 5832",
                "Vehicle" to "Honda CB Hornet 160R (2019)",
                "Owner" to "Ashutosh Dubey",
                "Address" to "Flat 4B, Skyline Residency, Indiranagar, BLR",
                "Engine Number" to "JC74EXXXXXXXX",
                "Chassis Number" to "ME4JC74BXKXXXXXXXX",
                "Insurance Expiry" to "27 October 2025",
                "Fitness Certificate" to "Valid until 27 Oct 2034",
            ),
        ))
    }

    // ── Tax domain ────────────────────────────────────────────────────────────

    private suspend fun seedTax(profileId: String) {
        val itrFy24 = objectRepository.createObject(
            profileId = profileId,
            objectType = "Tax Filing",
            domain = "Tax",
            title = "ITR-1 Filing — FY 2023-24",
            description = "Income Tax Return for Assessment Year 2024-25",
        )
        meta(itrFy24.objectId, "assessment_year", "2024-25", verified = true)
        meta(itrFy24.objectId, "financial_year", "2023-24", verified = true)
        meta(itrFy24.objectId, "itr_type", "ITR-1 (Sahaj) — Salary income")
        meta(itrFy24.objectId, "gross_total_income", "₹18,00,000")
        meta(itrFy24.objectId, "taxable_income", "₹13,50,000")
        meta(itrFy24.objectId, "tax_payable", "₹1,68,000")
        meta(itrFy24.objectId, "tds_deducted", "₹1,74,000")
        meta(itrFy24.objectId, "refund_due", "₹6,000")
        meta(itrFy24.objectId, "filing_date", "2024-07-15", verified = true)
        meta(itrFy24.objectId, "acknowledgement_number", "482739XXXXXXXX24")
        meta(itrFy24.objectId, "status", "Filed and verified (e-verified via Aadhaar OTP)")
        meta(itrFy24.objectId, "refund_status", "Received ₹6,000 in HDFC account — 20 Sep 2024")
        docQueue.add(DocSpec(
            objectId = itrFy24.objectId,
            fileName = "ITR1_AY2024_25_Acknowledgement.jpg",
            docLabel = "ITR Acknowledgement — AY 2024-25",
            headerColor = Color.parseColor("#006064"),
            fields = listOf(
                "Assessment Year" to "2024-25 (FY 2023-24)",
                "Taxpayer" to "Ashutosh Dubey",
                "PAN" to "ABCPD1234E",
                "ITR Form" to "ITR-1 (Sahaj)",
                "Gross Total Income" to "₹18,00,000",
                "Taxable Income" to "₹13,50,000",
                "Tax Liability" to "₹1,68,000",
                "TDS Deducted" to "₹1,74,000  |  Refund: ₹6,000",
                "Acknowledgement No." to "482739XXXXXXXX24",
                "Filing Date" to "15 July 2024",
            ),
        ))

        val itrFy23 = objectRepository.createObject(
            profileId = profileId,
            objectType = "Tax Filing",
            domain = "Tax",
            title = "ITR-1 Filing — FY 2022-23",
            description = "Income Tax Return for Assessment Year 2023-24",
        )
        meta(itrFy23.objectId, "assessment_year", "2023-24")
        meta(itrFy23.objectId, "financial_year", "2022-23")
        meta(itrFy23.objectId, "itr_type", "ITR-1 (Sahaj)")
        meta(itrFy23.objectId, "gross_total_income", "₹15,50,000")
        meta(itrFy23.objectId, "taxable_income", "₹11,50,000")
        meta(itrFy23.objectId, "tds_deducted", "₹1,20,000")
        meta(itrFy23.objectId, "filing_date", "2023-07-28")
        meta(itrFy23.objectId, "acknowledgement_number", "374829XXXXXXXX23")
        meta(itrFy23.objectId, "status", "Filed and verified — ITR-V submitted")

        val form16 = objectRepository.createObject(
            profileId = profileId,
            objectType = "Tax Document",
            domain = "Tax",
            title = "Form 16 — Accenture FY 2023-24",
            description = "TDS certificate issued by employer for salary income",
        )
        meta(form16.objectId, "document_type", "Form 16 — TDS Certificate")
        meta(form16.objectId, "issuer", "Accenture Technology Solutions India Pvt. Ltd.", verified = true)
        meta(form16.objectId, "employer_tan", "BLRE08XXXXX")
        meta(form16.objectId, "financial_year", "2023-24", verified = true)
        meta(form16.objectId, "gross_salary", "₹18,00,000")
        meta(form16.objectId, "tds_deducted", "₹1,74,000")
        meta(form16.objectId, "issue_date", "2024-06-10")
        meta(form16.objectId, "section_10_exemptions", "HRA ₹2,40,000 + LTA ₹50,000")
        meta(form16.objectId, "deductions_80c", "ELSS ₹1,50,000 + EPF ₹21,600")
        docQueue.add(DocSpec(
            objectId = form16.objectId,
            fileName = "Form_16_Accenture_FY2023_24.jpg",
            docLabel = "Form 16 — FY 2023-24",
            headerColor = Color.parseColor("#01579B"),
            fields = listOf(
                "Document" to "Form 16 — TDS Certificate (Part A & B)",
                "Employer" to "Accenture Technology Solutions India Pvt. Ltd.",
                "Employer TAN" to "BLRE08XXXXX",
                "Employee" to "Ashutosh Dubey",
                "PAN" to "ABCPD1234E",
                "Financial Year" to "2023-24",
                "Gross Salary" to "₹18,00,000",
                "TDS Deducted & Deposited" to "₹1,74,000",
                "Issue Date" to "10 June 2024",
            ),
        ))

        val gst = objectRepository.createObject(
            profileId = profileId,
            objectType = "Tax Document",
            domain = "Tax",
            title = "GST — Freelance Consulting",
            description = "GST registration for occasional freelance consulting income",
        )
        meta(gst.objectId, "document_type", "GST Registration Certificate")
        meta(gst.objectId, "gstin", "29ABCPD1234E1Z5", verified = true)
        meta(gst.objectId, "registration_date", "2023-09-01")
        meta(gst.objectId, "business_name", "Ashutosh Dubey — Consulting Services")
        meta(gst.objectId, "state", "Karnataka (State Code 29)")
        meta(gst.objectId, "turnover_threshold", "₹20 lakhs — below threshold, voluntary registration")
        meta(gst.objectId, "filing_frequency", "Quarterly (QRMP scheme)")
        meta(gst.objectId, "last_gstr1_filed", "Q3 FY 2023-24 — filed on time")
        reminder(
            objectId = gst.objectId,
            title = "GST return due — GSTR-3B",
            message = "Quarterly GST return (GSTR-3B) due — check Income Tax portal for current quarter deadline.",
            triggerDate = LocalDate.of(2025, 4, 22).toInstant(),
            priority = ReminderPriority.MEDIUM,
        )
    }

    // ── Domain life states ────────────────────────────────────────────────────

    private suspend fun seedDomainLifeStates(profileId: String) {
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Identity",
            currentSituation = "All core identity documents in order. Passport valid until March 2028. Aadhaar, PAN, Voter ID and Driving Licence are current and verified. UK visa was granted for the February London trip.",
            currentPriorities = listOf("Keep passport safe for London trip Feb 2025"),
            knownRisks = listOf("Passport expiry March 2028 — renew 6 months before travel"),
            openQuestions = emptyList(),
            recommendations = listOf("Set a passport renewal reminder for September 2027"),
            recentChanges = listOf("Identity documents seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Career",
            currentSituation = "Currently Senior Associate at Accenture Technology, Bengaluru. 5.5 years of experience. Previously at Infosys BPM. Resume targets PM and technology strategy roles. Notice period is 60 days.",
            currentPriorities = listOf("Deliver London client sprint (Feb 2025)", "Update resume with 2024 highlights"),
            knownRisks = listOf("60-day notice period must be factored into any transition timeline"),
            openQuestions = listOf("Are there any PM roles being actively pursued?"),
            recommendations = listOf("Add 2024 project highlights to resume before next application cycle"),
            recentChanges = listOf("Career profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Finance",
            currentSituation = "Salary account with HDFC (primary) and SBI (secondary). Star Health (₹5L) renewing Jan 2025. LIC Jeevan Amar term cover (₹1 Cr) until 2052. Active SIP portfolio via Zerodha — ₹10K/month, current value ~₹4.12L. EPF balance ~₹2.8L. HDFC Regalia credit card active. No active loans.",
            currentPriorities = listOf("Renew Star Health before 31 Jan 2025", "Pay HDFC Regalia by 5th"),
            knownRisks = listOf("Health insurance lapses if renewal missed", "Term premium due June"),
            openQuestions = listOf("Should Star Health sum insured be increased to ₹10L at renewal?"),
            recommendations = listOf("Increase health cover to ₹10L at renewal"),
            recentChanges = listOf("Finance profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Housing",
            currentSituation = "Renting 2 BHK in Indiranagar at ₹28,000/month. Lease expires Aug 2025. Security deposit ₹84,000 held. Family-owned home in Bilaspur occupied by parents.",
            currentPriorities = listOf("Begin lease renewal discussions with landlord by June 2025"),
            knownRisks = listOf("Lease expires Aug 2025 — initiate renewal well in advance"),
            openQuestions = listOf("Continue renting or consider purchasing a flat?"),
            recommendations = listOf("Start renewal conversation with landlord by June 2025"),
            recentChanges = listOf("Housing profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Health",
            currentSituation = "Blood group B+. No known allergies or chronic conditions. Last full checkup March 2024, BMI 23.4. Mild vision correction required. No current medications.",
            currentPriorities = listOf("Schedule annual checkup March 2025", "Renew vision prescription Jan 2025"),
            knownRisks = listOf("No recent dental record"),
            openQuestions = emptyList(),
            recommendations = listOf("Schedule full checkup March 2025", "Book dental checkup Q1 2025"),
            recentChanges = listOf("Health records seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Travel",
            currentSituation = "London business trip Feb 3–14 2025 fully arranged — UK visa granted, flights and hotel booked. Previous Dubai holiday Dec 2023 completed. Passport valid for all planned travel.",
            currentPriorities = listOf("Prepare for London trip", "Confirm forex arranged"),
            knownRisks = listOf("Travel insurance covers London trip — keep policy accessible"),
            openQuestions = listOf("Any personal travel planned for 2025?"),
            recommendations = listOf("Download boarding passes 24 hours before travel"),
            recentChanges = listOf("London trip visa grant confirmed"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Family",
            currentSituation = "Parents (Rajesh and Sunita Dubey) in Bilaspur family home. Mother is nominee on all major financial products. Father retired from government service.",
            currentPriorities = listOf("Ensure parents have adequate health insurance"),
            knownRisks = listOf("No senior citizen health insurance recorded for parents"),
            openQuestions = listOf("Should a senior citizen health cover be taken for parents?"),
            recommendations = listOf("Explore Star Health Senior Citizen Red Carpet for parents"),
            recentChanges = listOf("Family profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Education",
            currentSituation = "B.Tech CS from IIIT Delhi (2017), CGPA 8.2. AWS Solutions Architect cert valid until May 2026. PMP valid until Feb 2027. No active courses in progress.",
            currentPriorities = listOf("Accumulate PMP PDUs — 60 needed by Feb 2027"),
            knownRisks = listOf("AWS cert expires May 2026 — allow 3 months for renewal exam prep"),
            openQuestions = listOf("Any new certifications or courses being pursued?"),
            recommendations = listOf("Consider CKAD or Google Cloud certification"),
            recentChanges = listOf("Education profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Legal",
            currentSituation = "Employment contract with Accenture in force since Aug 2021. Tenancy agreement for Indiranagar flat registered, valid until Aug 2025. Mutual NDA signed for London client engagement. General PoA granted to father for Bilaspur property matters.",
            currentPriorities = listOf("Track tenancy agreement renewal (Aug 2025)", "Ensure NDA obligations are respected throughout London project"),
            knownRisks = listOf("Non-compete clause applies 12 months after leaving Accenture", "Tenancy agreement needs re-registration at renewal"),
            openQuestions = listOf("Should a personal will be drafted?"),
            recommendations = listOf("Draft a basic will naming nominees and assets", "Track non-compete scope before any career move"),
            recentChanges = listOf("Legal profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Vehicles",
            currentSituation = "One vehicle: Honda CB Hornet 160R (KA01 MJ 5832, 2019). Insurance (Bajaj Allianz) expires Oct 2025. PUC expires Feb 2025. Odometer at ~22,400 km. No loan or hypothecation on vehicle.",
            currentPriorities = listOf("Renew PUC before Feb 2025", "Budget for bike insurance renewal Oct 2025"),
            knownRisks = listOf("PUC expiry is imminent — illegal to ride without valid PUC", "Riding without valid insurance is an offence under Motor Vehicles Act"),
            openQuestions = listOf("Is the current OD+TP cover adequate or should zero-dep be added?"),
            recommendations = listOf("Renew PUC immediately at nearest centre", "Consider zero-depreciation add-on at next insurance renewal"),
            recentChanges = listOf("Vehicle profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
        domainRepository.upsertDomainLifeState(DomainLifeState(
            profileId = profileId, domain = "Tax",
            currentSituation = "ITR-1 filed for AY 2024-25 on 15 Jul 2024, refund of ₹6,000 received. Previous ITR (AY 2023-24) also filed and verified. Form 16 received from Accenture for FY 2023-24. GST registration active (QRMP scheme) for freelance consulting income. No outstanding tax demand.",
            currentPriorities = listOf("File ITR for AY 2025-26 by July 2025", "File quarterly GSTR-3B on time"),
            knownRisks = listOf("Freelance income must be declared even if below GST threshold", "Late ITR filing attracts ₹5,000 penalty under section 234F"),
            openQuestions = listOf("Should Old or New Tax Regime be chosen for FY 2024-25?", "Is 80C planning adequate?"),
            recommendations = listOf("Maximise 80C via ELSS/EPF before March 2025", "Compare old vs new regime before filing AY 2025-26 ITR"),
            recentChanges = listOf("Tax profile seeded on setup"),
            lastUpdated = Instant.now(), version = 1,
        ))
    }

    // ── Document generation ───────────────────────────────────────────────────

    private suspend fun seedDocuments() {
        val docsDir = File(context.filesDir, "demo_docs").also { it.mkdirs() }
        for (spec in docQueue) {
            try {
                val file = createDocumentImage(docsDir, spec)
                uploadDocumentUseCase(
                    objectId = spec.objectId,
                    filePath = file.absolutePath,
                    originalName = spec.fileName,
                    mimeType = "image/jpeg",
                    documentType = "IMAGE",
                )
                Timber.d("SampleDataSeeder: seeded document ${spec.fileName}")
            } catch (e: Exception) {
                Timber.w(e, "SampleDataSeeder: failed to seed document ${spec.fileName}")
            }
        }
    }

    private fun createDocumentImage(docsDir: File, spec: DocSpec): File {
        val W = 1240
        val H = 1754  // A4 proportions
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        // Header bar
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.headerColor }
        canvas.drawRect(0f, 0f, W.toFloat(), 200f, headerPaint)

        // Header accent strip
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 30
        }
        canvas.drawRect(0f, 180f, W.toFloat(), 200f, accentPaint)

        // Document label (small, top of header)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(spec.docLabel.uppercase(), 60f, 60f, labelPaint)

        // Document title (large)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val title = spec.fileName.removeSuffix(".jpg").replace("_", " ")
        canvas.drawText(title, 60f, 135f, titlePaint)

        // LifePilot branding dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 180
        }
        canvas.drawCircle(W - 80f, 100f, 28f, dotPaint)
        val lpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.headerColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("LP", W - 94f, 108f, lpPaint)

        // Field rows
        var y = 270f
        val fieldLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 28f
        }
        val fieldValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
        }
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.headerColor
            alpha = 12
        }

        spec.fields.forEachIndexed { idx, (label, value) ->
            if (idx % 2 == 0) {
                canvas.drawRect(0f, y - 10f, W.toFloat(), y + 90f, highlightPaint)
            }
            canvas.drawText(label, 60f, y + 28f, fieldLabelPaint)
            canvas.drawText(value, 60f, y + 70f, fieldValuePaint)
            y += 105f
            canvas.drawRect(40f, y - 5f, W - 40f, y - 4f, dividerPaint)
        }

        // "DEMO" watermark — faint diagonal
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFEFEF")
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.save()
        canvas.rotate(-35f, W / 2f, H / 2f)
        canvas.drawText("SAMPLE DOCUMENT", W / 2f - 380f, H / 2f, wmPaint)
        canvas.restore()

        // Footer bar
        val footerPaint = Paint().apply { color = Color.parseColor("#F9F9F9") }
        canvas.drawRect(0f, (H - 80).toFloat(), W.toFloat(), H.toFloat(), footerPaint)
        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BDBDBD")
            textSize = 24f
        }
        canvas.drawText("LifePilot — Your Administrative OS", 60f, (H - 30).toFloat(), footerTextPaint)
        canvas.drawText("Demo Data — ${java.time.LocalDate.now()}", W - 400f, (H - 30).toFloat(), footerTextPaint)

        val file = File(docsDir, spec.fileName)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        return file
    }

    // ── API key (demo builds only) ────────────────────────────────────────────

    private suspend fun seedDemoApiKey() {
        val key = BuildConfig.DEMO_API_KEY
        if (key.isBlank()) return
        val existing = preferenceManager.aiApiKey.firstOrNull()
        if (!existing.isNullOrBlank()) return
        preferenceManager.setAiApiKeyDirect(key)
        preferenceManager.setAiProvider("nvidia")
        preferenceManager.setAiModel("meta/llama-3.1-70b-instruct")
        Timber.d("SampleDataSeeder: demo API key configured")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun meta(objectId: String, fieldId: String, value: String, verified: Boolean = false) {
        metadataRepository.upsertMetadata(
            objectId = objectId,
            fieldId = fieldId,
            value = value,
            source = MetadataSource.USER,
            confidence = if (verified) 1.0f else 0.9f,
            verificationStatus = if (verified) VerificationStatus.VERIFIED else VerificationStatus.UNVERIFIED,
        )
    }

    private suspend fun reminder(objectId: String, title: String, message: String, triggerDate: Instant, priority: ReminderPriority) {
        reminderRepository.createReminder(Reminder(
            reminderId = UUID.randomUUID().toString(),
            objectId = objectId,
            reminderType = "EXPIRY",
            triggerDate = triggerDate,
            priority = priority,
            status = ReminderStatus.SCHEDULED,
            title = title,
            message = message,
        ))
    }

    private fun LocalDate.toInstant(): Instant = atStartOfDay(ZoneId.systemDefault()).toInstant()
}
