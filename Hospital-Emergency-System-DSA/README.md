# Hospital Emergency & Patient Priority Management System

An end-to-end Java full-stack web application designed for hospital triage, emergency patient prioritization (inspired by surge scenarios like COVID-19 ward/ICU shortages), doctor allocation, treatment tracking, and report generation.

## Architecture & Technologies
- **Backend:** Pure Java (`com.sun.net.httpserver.HttpServer`, JDK 17+)
- **Custom Data Structures & Algorithms (100% Java):**
  - **Custom Hash Map (`src/dsa/CustomHashMap.java`):** Separate-chaining hash map for $O(1)$ average-case patient and doctor lookup by ID.
  - **Custom Min-Heap & Priority Queue (`src/dsa/EmergencyMinHeap.java`):** Binary Min-Heap ordering emergency patients by severity level (`1 = Critical / Resuscitation`, `2 = Severe / Urgent`, `3 = Moderate`, `4 = Mild`) with arrival-order tie-breaking and dynamic priority escalation.
  - **Custom FIFO Queue (`src/dsa/DoctorWaitingQueue.java`):** Circular/linked FIFO queue managing available on-duty doctors ready for immediate triage assignment.
  - **Custom Doubly Linked List (`src/dsa/TreatmentLinkedList.java`):** Doubly linked list tracking active treatment patients and discharged/completed patient records for reporting and traversal.
- **Frontend:** Responsive single-page dashboard (`web/index.html`, `web/style.css`, `web/app.js`) with clean hospital UI (no DSA jargon exposed on the interface).
- **Database:** MySQL (`sql/schema.sql` via JDBC) with automatic local file persistence fallback (`data/`) so the project compiles and runs immediately out-of-the-box on any Windows/Linux laptop.

## How to Run on Windows (VS Code)
1. Open the extracted `Hospital-Emergency-System-DSA` folder directly in **VS Code**.
2. Open the VS Code terminal (`Ctrl + ~`) and run:
   ```powershell
   .\run.bat
   ```
3. Open your browser to **`http://localhost:8080`**.
