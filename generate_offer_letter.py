"""
Generates a demo offer letter PDF for LifePilot recruiter showcase.
Clearly marked as DEMO / SAMPLE throughout.
Run: python3 generate_offer_letter.py
Output: NovaPay_Offer_Letter_Ashutosh_Dubey.pdf
"""

from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT, TA_JUSTIFY
import os

OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "NovaPay_Offer_Letter_Ashutosh_Dubey.pdf")

W, H = A4

# Brand colours
NOVAPAY_NAVY   = colors.HexColor("#0D1B4B")
NOVAPAY_TEAL   = colors.HexColor("#00A8A8")
NOVAPAY_LIGHT  = colors.HexColor("#E8F5F5")
DEMO_WATERMARK = colors.HexColor("#EEEEEE")
BODY_GREY      = colors.HexColor("#333333")
LABEL_GREY     = colors.HexColor("#888888")

styles = getSampleStyleSheet()


def make_style(name, parent="Normal", **kwargs):
    return ParagraphStyle(name, parent=styles[parent], **kwargs)


# ── Paragraph styles ─────────────────────────────────────────────────────────
H1 = make_style("H1", fontSize=22, textColor=NOVAPAY_NAVY, spaceAfter=2,
                fontName="Helvetica-Bold", leading=26)
H2 = make_style("H2", fontSize=13, textColor=NOVAPAY_NAVY, spaceAfter=2,
                fontName="Helvetica-Bold", leading=17)
BODY = make_style("BODY", fontSize=10, textColor=BODY_GREY, leading=15,
                  spaceAfter=6, alignment=TA_JUSTIFY)
BODY_L = make_style("BODY_L", fontSize=10, textColor=BODY_GREY, leading=15,
                    spaceAfter=4, alignment=TA_LEFT)
SMALL = make_style("SMALL", fontSize=8, textColor=LABEL_GREY, leading=11)
SMALL_C = make_style("SMALL_C", fontSize=8, textColor=LABEL_GREY, leading=11,
                     alignment=TA_CENTER)
DEMO_TAG = make_style("DEMO_TAG", fontSize=8, textColor=colors.white,
                      fontName="Helvetica-Bold", alignment=TA_CENTER)
REF = make_style("REF", fontSize=9, textColor=LABEL_GREY, alignment=TA_RIGHT,
                 spaceAfter=2)
SIGN_NAME = make_style("SIGN_NAME", fontSize=11, textColor=NOVAPAY_NAVY,
                       fontName="Helvetica-Bold", leading=14)
SIGN_TITLE = make_style("SIGN_TITLE", fontSize=9, textColor=LABEL_GREY, leading=12)
FOOTER_S = make_style("FOOTER_S", fontSize=7.5, textColor=LABEL_GREY,
                      alignment=TA_CENTER, leading=10)


def build():
    doc = SimpleDocTemplate(
        OUTPUT_PATH,
        pagesize=A4,
        leftMargin=22*mm,
        rightMargin=22*mm,
        topMargin=10*mm,
        bottomMargin=20*mm,
        title="Offer of Employment — NovaPay Technologies",
        author="NovaPay Technologies Pvt. Ltd. [DEMO]",
        subject="Offer Letter — Ashutosh Dubey [SAMPLE DOCUMENT — FOR DEMO PURPOSES ONLY]",
    )

    story = []

    # ── DEMO BANNER ──────────────────────────────────────────────────────────
    demo_data = [[Paragraph("⚠  SAMPLE DOCUMENT — FOR DEMO / TESTING PURPOSES ONLY  ⚠", DEMO_TAG)]]
    demo_tbl = Table(demo_data, colWidths=[166*mm])
    demo_tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#C62828")),
        ("TOPPADDING",    (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING",   (0, 0), (-1, -1), 8),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 8),
        ("ROUNDEDCORNERS", [4]),
    ]))
    story.append(demo_tbl)
    story.append(Spacer(1, 6*mm))

    # ── HEADER — Logo area ───────────────────────────────────────────────────
    logo_row = [
        [
            Paragraph("<font color='#0D1B4B'><b>Nova</b></font>"
                      "<font color='#00A8A8'><b>Pay</b></font>", make_style(
                "LOGO", fontSize=28, fontName="Helvetica-Bold", leading=32)),
            Paragraph("NovaPay Technologies Pvt. Ltd.<br/>"
                      "Level 9, Prestige Trade Tower, Palace Road,<br/>"
                      "Bengaluru, Karnataka 560 001<br/>"
                      "CIN: U72900KA2019PTC123456  |  GST: 29AABCN1234D1Z5",
                      make_style("ADDR", fontSize=8, textColor=LABEL_GREY,
                                 leading=12, alignment=TA_RIGHT)),
        ]
    ]
    logo_tbl = Table(logo_row, colWidths=[80*mm, 86*mm])
    logo_tbl.setStyle(TableStyle([
        ("VALIGN",       (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING",  (0, 0), (-1, -1), 0),
        ("RIGHTPADDING", (0, 0), (-1, -1), 0),
    ]))
    story.append(logo_tbl)
    story.append(Spacer(1, 3*mm))

    # Teal divider
    story.append(HRFlowable(width="100%", thickness=3, color=NOVAPAY_TEAL,
                             spaceAfter=4*mm))

    # ── REF + DATE ───────────────────────────────────────────────────────────
    ref_row = [[
        Paragraph("Ref: NP/HR/OL/2026/0047", SMALL),
        Paragraph("Date: 01 July 2026", REF),
    ]]
    ref_tbl = Table(ref_row, colWidths=[83*mm, 83*mm])
    ref_tbl.setStyle(TableStyle([
        ("LEFTPADDING",  (0, 0), (-1, -1), 0),
        ("RIGHTPADDING", (0, 0), (-1, -1), 0),
        ("BOTTOMPADDING",(0, 0), (-1, -1), 0),
    ]))
    story.append(ref_tbl)
    story.append(Spacer(1, 5*mm))

    # ── SALUTATION ───────────────────────────────────────────────────────────
    story.append(Paragraph("Mr. Ashutosh Dubey", H2))
    story.append(Paragraph(
        "Flat 4B, Skyline Residency, 12th Main, Indiranagar,<br/>"
        "Bengaluru, Karnataka 560 034",
        BODY_L))
    story.append(Spacer(1, 5*mm))

    story.append(Paragraph("Dear Ashutosh,", BODY_L))
    story.append(Spacer(1, 3*mm))

    # ── SUBJECT LINE ─────────────────────────────────────────────────────────
    story.append(Paragraph(
        "<b>Subject: Offer of Employment — Senior Product Manager, Payments Platform</b>",
        make_style("SUBJ", fontSize=11, textColor=NOVAPAY_NAVY, leading=14,
                   fontName="Helvetica-Bold", spaceAfter=4)))
    story.append(HRFlowable(width="100%", thickness=0.5, color=NOVAPAY_TEAL,
                             spaceAfter=4*mm))

    # ── OPENING PARAGRAPH ───────────────────────────────────────────────────
    story.append(Paragraph(
        "We are delighted to extend this offer of employment to you for the position of "
        "<b>Senior Product Manager — Payments Platform</b> at NovaPay Technologies Pvt. Ltd. "
        "Following your interactions with our hiring panel and subsequent deliberations, we are "
        "confident that your background in technology consulting, AI systems, and product strategy "
        "makes you an excellent fit for the role and our mission.",
        BODY))
    story.append(Paragraph(
        "This offer is subject to the terms and conditions set forth below, successful completion "
        "of reference checks, and submission of all required documentation prior to your joining date.",
        BODY))
    story.append(Spacer(1, 4*mm))

    # ── COMPENSATION TABLE ───────────────────────────────────────────────────
    story.append(Paragraph("1. Compensation & Benefits", H2))

    comp_data = [
        ["Component", "Annual (₹)", "Monthly (₹)"],
        ["Basic Salary", "12,00,000", "1,00,000"],
        ["House Rent Allowance (HRA)", "4,80,000", "40,000"],
        ["Special Allowance", "3,00,000", "25,000"],
        ["Performance Bonus (target)", "2,40,000", "20,000"],
        ["Employer PF Contribution", "1,03,680", "8,640"],
        ["Gratuity Provision", "57,693", "4,808"],
        ["", "", ""],
        ["Total Cost to Company (CTC)", "23,81,373", "1,98,448"],
        ["Fixed Take-Home (post-TDS est.)", "—", "~1,55,000"],
    ]

    comp_style = TableStyle([
        # Header row
        ("BACKGROUND",    (0, 0), (-1, 0),  NOVAPAY_NAVY),
        ("TEXTCOLOR",     (0, 0), (-1, 0),  colors.white),
        ("FONTNAME",      (0, 0), (-1, 0),  "Helvetica-Bold"),
        ("FONTSIZE",      (0, 0), (-1, 0),  9),
        ("BOTTOMPADDING", (0, 0), (-1, 0),  6),
        ("TOPPADDING",    (0, 0), (-1, 0),  6),
        # Alternating rows
        ("BACKGROUND",    (0, 1), (-1, 1),  NOVAPAY_LIGHT),
        ("BACKGROUND",    (0, 3), (-1, 3),  NOVAPAY_LIGHT),
        ("BACKGROUND",    (0, 5), (-1, 5),  NOVAPAY_LIGHT),
        ("BACKGROUND",    (0, 7), (-1, 7),  NOVAPAY_LIGHT),
        # Total row
        ("BACKGROUND",    (0, 8), (-1, 8),  colors.HexColor("#CCE5E5")),
        ("FONTNAME",      (0, 8), (-1, 8),  "Helvetica-Bold"),
        ("FONTNAME",      (0, 9), (-1, 9),  "Helvetica-Oblique"),
        ("TEXTCOLOR",     (0, 9), (-1, 9),  LABEL_GREY),
        # Alignment
        ("ALIGN",         (1, 0), (-1, -1), "RIGHT"),
        ("ALIGN",         (0, 0), (0, -1),  "LEFT"),
        ("FONTSIZE",      (0, 1), (-1, -1), 9),
        ("TOPPADDING",    (0, 1), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 1), (-1, -1), 4),
        ("LEFTPADDING",   (0, 0), (-1, -1), 6),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 6),
        ("GRID",          (0, 0), (-1, -2), 0.3, colors.HexColor("#CCCCCC")),
        ("LINEBELOW",     (0, 7), (-1, 7),  1, NOVAPAY_TEAL),
    ])

    comp_tbl = Table(comp_data, colWidths=[90*mm, 40*mm, 36*mm])
    comp_tbl.setStyle(comp_style)
    story.append(comp_tbl)
    story.append(Spacer(1, 5*mm))

    # ── EQUITY ───────────────────────────────────────────────────────────────
    story.append(Paragraph(
        "<b>ESOPs:</b> You will be eligible for an Employee Stock Option Plan grant of "
        "<b>1,200 options</b> (representing ~0.012% of fully diluted equity) at an exercise "
        "price of ₹10 per share, vesting over 4 years with a 1-year cliff, subject to the "
        "Company's ESOP policy and Board approval.",
        BODY))
    story.append(Spacer(1, 4*mm))

    # ── TERMS TABLE ──────────────────────────────────────────────────────────
    story.append(Paragraph("2. Terms of Employment", H2))

    terms_data = [
        ["Designation",          "Senior Product Manager — Payments Platform"],
        ["Department",           "Product Management"],
        ["Reporting To",         "VP of Product, Mr. Karan Mehta"],
        ["Work Location",        "NovaPay HQ, Prestige Trade Tower, Bengaluru (Hybrid — 3 days on-site)"],
        ["Date of Joining",      "01 September 2026"],
        ["Employment Type",      "Full-time, Permanent"],
        ["Probation Period",     "3 months from date of joining"],
        ["Notice Period",        "60 days (both sides) post probation; 30 days during probation"],
        ["Annual Leave",         "18 days paid leave + 10 public holidays + 7 sick days per year"],
        ["Health Insurance",     "Group Mediclaim — ₹5,00,000 per annum (employee + spouse + 2 children)"],
        ["Travel Allowance",     "Cab-on-demand within Bengaluru for office commute"],
        ["Laptop & Peripherals", "MacBook Pro 14\" (M3) + peripherals — company asset"],
    ]
    terms_style = TableStyle([
        ("FONTSIZE",      (0, 0), (-1, -1), 9),
        ("FONTNAME",      (0, 0), (0, -1),  "Helvetica-Bold"),
        ("TEXTCOLOR",     (0, 0), (0, -1),  NOVAPAY_NAVY),
        ("VALIGN",        (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING",    (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("LEFTPADDING",   (0, 0), (-1, -1), 6),
        ("RIGHTPADDING",  (0, 0), (-1, -1), 6),
        ("ROWBACKGROUNDS",(0, 0), (-1, -1), [colors.white, NOVAPAY_LIGHT]),
        ("GRID",          (0, 0), (-1, -1), 0.3, colors.HexColor("#DDDDDD")),
    ])
    terms_tbl = Table(terms_data, colWidths=[52*mm, 114*mm])
    terms_tbl.setStyle(terms_style)
    story.append(terms_tbl)
    story.append(Spacer(1, 5*mm))

    # ── CONDITIONS ───────────────────────────────────────────────────────────
    story.append(Paragraph("3. Conditions of this Offer", H2))
    conditions = [
        "Submission of educational qualification certificates, PAN card, Aadhaar card, last 3 months' "
        "salary slips, and Form 16 from your current employer, within 7 days of joining.",
        "Successful background verification (BGV) including employment history, education, and "
        "criminal records check, conducted by our third-party partner AuthBridge.",
        "No existing non-compete, non-solicitation, or intellectual property assignment agreement "
        "that would restrict your ability to take up this role. <i>Please review your existing "
        "employment contract carefully before accepting.</i>",
        "Serving or negotiating full notice period with your current employer. Any shortfall in "
        "notice period buyout will be the candidate's responsibility.",
        "This offer is valid for acceptance until <b>10 July 2026</b>. Failure to revert by this "
        "date will render the offer null and void.",
    ]
    for i, cond in enumerate(conditions, 1):
        story.append(Paragraph(f"{i}.&nbsp;&nbsp;{cond}", BODY_L))
    story.append(Spacer(1, 4*mm))

    # ── CONFIDENTIALITY ───────────────────────────────────────────────────────
    story.append(Paragraph("4. Confidentiality & Intellectual Property", H2))
    story.append(Paragraph(
        "By accepting this offer, you agree to execute the Company's standard Confidentiality, "
        "Non-Disclosure, and Intellectual Property Assignment Agreement on or before your first day "
        "of employment. All work product, inventions, and discoveries made during the course of "
        "employment shall be the sole property of NovaPay Technologies Pvt. Ltd.",
        BODY))
    story.append(Spacer(1, 5*mm))

    # ── ACCEPTANCE ───────────────────────────────────────────────────────────
    story.append(Paragraph("5. Acceptance", H2))
    story.append(Paragraph(
        "To accept this offer, please sign and return a copy of this letter along with the enclosed "
        "Personal Data Form to <b>hr@novapay.in</b> by <b>10 July 2026</b>. Digital signature via "
        "DigiLocker or Aadhaar eSign is accepted.",
        BODY))
    story.append(Paragraph(
        "We look forward to welcoming you to the NovaPay family. Should you have any queries, "
        "please reach out to your HR Partner, Ms. Divya Sharma, at <b>divya.sharma@novapay.in</b> "
        "or +91-80-4567-8901.",
        BODY))
    story.append(Spacer(1, 7*mm))

    # ── SIGNATURES ───────────────────────────────────────────────────────────
    sig_data = [[
        Paragraph("For NovaPay Technologies Pvt. Ltd.", SMALL),
        Paragraph("Accepted by", SMALL),
    ]]
    sig_tbl1 = Table(sig_data, colWidths=[83*mm, 83*mm])
    sig_tbl1.setStyle(TableStyle([("LEFTPADDING", (0,0),(-1,-1), 0),
                                   ("RIGHTPADDING",(0,0),(-1,-1), 0)]))
    story.append(sig_tbl1)
    story.append(Spacer(1, 12*mm))

    sig_data2 = [[
        Paragraph("Rohan Kapoor<br/>Chief Human Resources Officer<br/>"
                  "NovaPay Technologies Pvt. Ltd.", SIGN_TITLE),
        Paragraph("Ashutosh Dubey<br/>Date: _______________", SIGN_TITLE),
    ]]
    sig_tbl2 = Table(sig_data2, colWidths=[83*mm, 83*mm])
    sig_tbl2.setStyle(TableStyle([("LEFTPADDING", (0,0),(-1,-1), 0),
                                   ("RIGHTPADDING",(0,0),(-1,-1), 0),
                                   ("VALIGN", (0,0),(-1,-1), "TOP")]))
    story.append(sig_tbl2)
    story.append(Spacer(1, 8*mm))

    # ── FOOTER ───────────────────────────────────────────────────────────────
    story.append(HRFlowable(width="100%", thickness=0.5, color=NOVAPAY_TEAL,
                             spaceBefore=2*mm, spaceAfter=3*mm))

    footer_row = [[
        Paragraph("⚠ SAMPLE DOCUMENT — FOR DEMO / TESTING PURPOSES ONLY ⚠\n"
                  "NovaPay Technologies Pvt. Ltd. is a fictitious company created for "
                  "LifePilot demo purposes.\nThis document does not represent a real "
                  "employment offer and should not be used as one.", FOOTER_S),
        Paragraph("NovaPay Technologies Pvt. Ltd.\nLevel 9, Prestige Trade Tower, Bengaluru 560 001\n"
                  "hr@novapay.in  |  +91-80-4567-8901\nwww.novapay.in [fictitious]", FOOTER_S),
    ]]
    footer_tbl = Table(footer_row, colWidths=[100*mm, 66*mm])
    footer_tbl.setStyle(TableStyle([
        ("LEFTPADDING",  (0, 0), (-1, -1), 0),
        ("RIGHTPADDING", (0, 0), (-1, -1), 0),
        ("VALIGN",       (0, 0), (-1, -1), "TOP"),
    ]))
    story.append(footer_tbl)

    # ── BUILD ─────────────────────────────────────────────────────────────────
    def on_page(canvas_obj, doc):
        """Faint diagonal watermark on every page."""
        canvas_obj.saveState()
        canvas_obj.setFont("Helvetica-Bold", 52)
        canvas_obj.setFillColor(DEMO_WATERMARK)
        canvas_obj.translate(W / 2, H / 2)
        canvas_obj.rotate(40)
        canvas_obj.drawCentredString(0, 0, "SAMPLE DOCUMENT")
        canvas_obj.restoreState()

        # Page number
        canvas_obj.setFont("Helvetica", 7)
        canvas_obj.setFillColor(LABEL_GREY)
        canvas_obj.drawCentredString(W / 2, 10*mm,
            f"NovaPay Technologies — Confidential | Page {doc.page}")

    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
    print(f"Generated: {OUTPUT_PATH}")


if __name__ == "__main__":
    build()
