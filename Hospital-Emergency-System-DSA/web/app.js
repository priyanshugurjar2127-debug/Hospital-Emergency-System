async function refreshHospitalState() {
  const res = await fetch('/api/state');
  const data = await res.json();

  document.getElementById('dbStatus').textContent = data.dbStatus;
  document.getElementById('kpiWaiting').textContent = data.waitingCount;
  document.getElementById('kpiCritical').textContent = data.criticalCount;
  document.getElementById('kpiInTreatment').textContent = data.inTreatmentCount;
  document.getElementById('kpiDoctors').textContent = `${data.availableDoctorsCount} / ${data.totalDoctorsCount}`;

  renderDoctors(data.doctors);
  renderWaitingList(data.waitingList, false);
  renderTreatmentList(data.inTreatmentList);
  renderReport(data);
}

function renderDoctors(doctors) {
  const tbody = document.getElementById('doctorsBody');
  tbody.innerHTML = '';
  doctors.forEach(d => {
    const isAvail = d.status === 'AVAILABLE';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${d.doctorId}</strong></td>
      <td>${d.name}</td>
      <td>${d.specialty}</td>
      <td><span class="${isAvail ? 'doc-avail' : 'doc-busy'}">${isAvail ? 'Available' : 'In Consultation'}</span></td>
      <td>${d.currentPatientId ? d.currentPatientId : '—'}</td>
    `;
    tbody.appendChild(tr);
  });
}

function renderWaitingList(patients, isSearchResult) {
  const tbody = document.getElementById('waitingBody');
  tbody.innerHTML = '';
  if (patients.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8">${isSearchResult ? 'No matching patient records found.' : 'No patients waiting in triage.'}</td></tr>`;
    return;
  }

  patients.forEach(p => {
    const tr = document.createElement('tr');
    const isWaiting = p.status === 'WAITING';
    const posText = isWaiting ? `#${p.queuePosition}` : p.status.replace('_', ' ');
    const waitText = isWaiting ? `~${p.estimatedWaitMins} mins` : '—';

    const priorityControl = isWaiting
      ? `<select onchange="updatePatientPriority('${p.patientId}', this.value)" aria-label="Update Priority">
           <option value="1" ${p.emergencyLevel === 1 ? 'selected' : ''}>Level 1 (Critical)</option>
           <option value="2" ${p.emergencyLevel === 2 ? 'selected' : ''}>Level 2 (Severe)</option>
           <option value="3" ${p.emergencyLevel === 3 ? 'selected' : ''}>Level 3 (Moderate)</option>
           <option value="4" ${p.emergencyLevel === 4 ? 'selected' : ''}>Level 4 (Mild)</option>
         </select>`
      : `<span class="doc-busy">${p.status.replace('_', ' ')}</span>`;

    tr.innerHTML = `
      <td><strong>${posText}</strong></td>
      <td><strong>${p.patientId}</strong></td>
      <td>${p.name} <span style="color:#64748b">(${p.age}y)</span></td>
      <td>${p.symptoms}</td>
      <td><span class="sev-${p.emergencyLevel}">${p.priorityLabel}</span></td>
      <td>${waitText}</td>
      <td>${p.registeredAt}</td>
      <td>${priorityControl}</td>
    `;
    tbody.appendChild(tr);
  });
}

function renderTreatmentList(patients) {
  const tbody = document.getElementById('treatmentBody');
  tbody.innerHTML = '';
  if (patients.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4">No patients currently in treatment rooms.</td></tr>';
    return;
  }
  patients.forEach(p => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${p.patientId}</strong> — ${p.name}</td>
      <td><span class="sev-${p.emergencyLevel}">${p.priorityLabel}</span></td>
      <td>${p.assignedDoctorName}</td>
      <td><button class="btn btn-primary btn-sm" onclick="dischargePatient('${p.patientId}')">Complete &amp; Discharge</button></td>
    `;
    tbody.appendChild(tr);
  });
}

function renderReport(data) {
  const box = document.getElementById('reportSummaryBox');
  box.innerHTML = `
    <span><strong>Total Registered:</strong> ${data.totalPatients}</span>
    <span><strong>Waiting:</strong> ${data.waitingCount}</span>
    <span><strong>In Care:</strong> ${data.inTreatmentCount}</span>
    <span><strong>Discharged:</strong> ${data.dischargedCount}</span>
  `;

  const tbody = document.getElementById('dischargedBody');
  tbody.innerHTML = '';
  if (data.dischargedList.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4">No patients discharged yet during this shift.</td></tr>';
    return;
  }
  data.dischargedList.forEach(p => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${p.patientId}</strong></td>
      <td>${p.name}</td>
      <td>${p.assignedDoctorName || 'Duty Physician'}</td>
      <td><span class="doc-avail">Discharged</span></td>
    `;
    tbody.appendChild(tr);
  });
}

document.getElementById('registerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const params = new URLSearchParams({
    name: document.getElementById('ptName').value,
    age: document.getElementById('ptAge').value,
    emergencyLevel: document.getElementById('ptLevel').value,
    patientId: document.getElementById('ptId').value,
    symptoms: document.getElementById('ptSymptoms').value
  });
  const res = await fetch('/api/patient/register', { method: 'POST', body: params });
  if (res.ok) {
    e.target.reset();
    document.getElementById('ptLevel').value = '3';
    refreshHospitalState();
  } else {
    const err = await res.json();
    alert(err.error || 'Could not register patient');
  }
});

document.getElementById('addDoctorForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const params = new URLSearchParams({
    name: document.getElementById('docName').value,
    specialty: document.getElementById('docSpec').value
  });
  const res = await fetch('/api/doctor/add', { method: 'POST', body: params });
  if (res.ok) {
    e.target.reset();
    refreshHospitalState();
  } else {
    const err = await res.json();
    alert(err.error || 'Could not add doctor');
  }
});

document.getElementById('assignNextBtn').addEventListener('click', async () => {
  const res = await fetch('/api/triage/assign-next', { method: 'POST' });
  const data = await res.json();
  if (!res.ok) {
    alert(data.error || 'Unable to assign patient');
  }
  refreshHospitalState();
});

async function updatePatientPriority(patientId, newLevel) {
  const params = new URLSearchParams({ patientId, emergencyLevel: newLevel });
  const res = await fetch('/api/patient/priority', { method: 'POST', body: params });
  if (res.ok) {
    refreshHospitalState();
  } else {
    const err = await res.json();
    alert(err.error || 'Priority update failed');
  }
}

async function dischargePatient(patientId) {
  const params = new URLSearchParams({ patientId });
  const res = await fetch('/api/patient/discharge', { method: 'POST', body: params });
  if (res.ok) {
    refreshHospitalState();
  } else {
    const err = await res.json();
    alert(err.error || 'Discharge failed');
  }
}

document.getElementById('lookupBtn').addEventListener('click', async () => {
  const q = document.getElementById('lookupInput').value.trim();
  if (!q) {
    refreshHospitalState();
    return;
  }
  const res = await fetch(`/api/patient/lookup?q=${encodeURIComponent(q)}`);
  const matches = await res.json();
  renderWaitingList(matches, true);
});

document.getElementById('resetLookupBtn').addEventListener('click', () => {
  document.getElementById('lookupInput').value = '';
  refreshHospitalState();
});

document.getElementById('printReportBtn').addEventListener('click', () => {
  window.print();
});

refreshHospitalState();
