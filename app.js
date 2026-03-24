// Attendance Management System - Revised Logic

class AttendanceSystem {
    constructor() {
        this.students = JSON.parse(localStorage.getItem('students')) || [];
        this.totalSessions = parseInt(localStorage.getItem('totalSessions')) || 20;
        this.currentView = 'dashboard';
        this.charts = {};
        this.init();
    }

    init() {
        this.cacheDOM();
        this.bindEvents();
        this.updateStats();
        this.renderAll();
        this.updateDate();
    }

    cacheDOM() {
        this.views = document.querySelectorAll('.view');
        this.navItems = document.querySelectorAll('.nav-item');
        this.studentListBody = document.getElementById('student-list-body');
        this.manageStudentListBody = document.getElementById('manage-student-list-body');
        this.studentForm = document.getElementById('student-form');
        this.studentModal = document.getElementById('student-modal');
        this.addStudentBtn = document.getElementById('add-student-btn');
        this.closeModalBtn = document.querySelector('.close-modal');
        this.globalSearch = document.getElementById('global-search');
        this.totalSessionsInput = document.getElementById('total-sessions-input');
        
        // Ensure input reflects state
        if (this.totalSessionsInput) {
            this.totalSessionsInput.value = this.totalSessions;
        }
        
        this.totalStudentsEl = document.getElementById('total-students');
        this.activeCheckinsEl = document.getElementById('active-checkins');
        this.avgAttendanceEl = document.getElementById('avg-attendance');
        this.currentDateEl = document.getElementById('current-date');
    }

    bindEvents() {
        if (this.addStudentBtn) this.addStudentBtn.addEventListener('click', () => this.showModal());
        if (this.closeModalBtn) this.closeModalBtn.addEventListener('click', () => this.hideModal());
        if (this.studentForm) this.studentForm.addEventListener('submit', (e) => this.handleAddStudent(e));
        
        this.navItems.forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const targetView = item.getAttribute('data-view');
                console.log('Switching to view:', targetView);
                this.switchView(targetView);
            });
        });

        if (this.globalSearch) {
            this.globalSearch.addEventListener('input', () => {
                console.log('Searching for:', this.globalSearch.value);
                this.renderAll();
            });
        }

        if (this.totalSessionsInput) {
            this.totalSessionsInput.addEventListener('change', (e) => {
                this.totalSessions = parseInt(e.target.value) || 1;
                localStorage.setItem('totalSessions', this.totalSessions);
                this.renderAll();
                this.updateStats();
            });
        }

        window.addEventListener('click', (e) => {
            if (e.target === this.studentModal) this.hideModal();
        });
    }

    switchView(viewId) {
        this.currentView = viewId;
        this.views.forEach(v => {
            v.classList.remove('active');
            if (v.id === `${viewId}-view`) v.classList.add('active');
        });
        
        this.navItems.forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('data-view') === viewId) item.classList.add('active');
        });

        if (viewId === 'analytics') this.renderCharts();
    }

    updateDate() {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        if (this.currentDateEl) {
            this.currentDateEl.textContent = new Date().toLocaleDateString('en-US', options);
        }
    }

    showModal() { if (this.studentModal) this.studentModal.style.display = 'block'; }
    hideModal() { 
        if (this.studentModal) this.studentModal.style.display = 'none'; 
        if (this.studentForm) this.studentForm.reset(); 
    }

    handleAddStudent(e) {
        e.preventDefault();
        const name = document.getElementById('name').value;
        const rollNo = document.getElementById('rollNo').value;
        const course = document.getElementById('course').value;

        if (this.students.some(s => s.rollNo === rollNo)) {
            alert('Roll number already exists!');
            return;
        }

        const newStudent = {
            name, rollNo, course,
            attendanceCount: 0,
            isCheckedIn: false
        };

        this.students.push(newStudent);
        this.saveData();
        this.hideModal();
        this.renderAll();
        this.updateStats();
    }

    deleteStudent(rollNo) {
        if (confirm('Are you sure you want to remove this student?')) {
            this.students = this.students.filter(s => s.rollNo !== rollNo);
            this.saveData();
            this.renderAll();
            this.updateStats();
        }
    }

    toggleAttendance(rollNo) {
        const student = this.students.find(s => s.rollNo === rollNo);
        if (!student) return;

        if (!student.isCheckedIn) {
            student.isCheckedIn = true;
        } else {
            student.isCheckedIn = false;
            student.attendanceCount++;
        }

        this.saveData();
        this.renderAll();
        this.updateStats();
    }

    saveData() {
        localStorage.setItem('students', JSON.stringify(this.students));
    }

    updateStats() {
        if (this.totalStudentsEl) this.totalStudentsEl.textContent = this.students.length;
        if (this.activeCheckinsEl) this.activeCheckinsEl.textContent = this.students.filter(s => s.isCheckedIn).length;
        
        const totalPossible = this.students.length * this.totalSessions;
        const totalActual = this.students.reduce((acc, s) => acc + s.attendanceCount, 0);
        const avg = totalPossible > 0 ? ((totalActual / totalPossible) * 100).toFixed(1) : 0;
        if (this.avgAttendanceEl) this.avgAttendanceEl.textContent = `${avg}%`;
    }

    renderAll() {
        const searchTerm = this.globalSearch ? this.globalSearch.value.toLowerCase() : '';
        const filtered = this.students.filter(s => 
            s.name.toLowerCase().includes(searchTerm) || 
            s.rollNo.toString().includes(searchTerm)
        );

        this.renderAttendance(filtered);
        this.renderManage(filtered);
    }

    renderAttendance(data) {
        if (!this.studentListBody) return;
        this.studentListBody.innerHTML = '';
        if (data.length === 0) {
            this.studentListBody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 40px;">No matching students.</td></tr>`;
            return;
        }

        data.forEach(student => {
            const perc = ((student.attendanceCount / this.totalSessions) * 100).toFixed(0);
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div class="avatar" style="width: 32px; height: 32px; font-size: 0.8rem;">${this.getInitials(student.name)}</div>
                        <span>${student.name}</span>
                    </div>
                </td>
                <td>#${student.rollNo}</td>
                <td><span class="badge ${student.isCheckedIn ? 'warning' : 'success'}">${student.isCheckedIn ? 'In Class' : 'Available'}</span></td>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <span style="min-width: 35px;">${perc}%</span>
                        <div class="progress-container"><div class="progress-bar" style="width: ${perc}%"></div></div>
                    </div>
                </td>
                <td>
                    <button onclick="app.toggleAttendance('${student.rollNo}')" class="btn btn-sm ${student.isCheckedIn ? 'btn-primary' : 'btn-outline'}">
                        ${student.isCheckedIn ? 'Check Out' : 'Check In'}
                    </button>
                </td>
            `;
            this.studentListBody.appendChild(tr);
        });
        if (window.lucide) lucide.createIcons();
    }

    renderManage(data) {
        if (!this.manageStudentListBody) return;
        this.manageStudentListBody.innerHTML = '';
        data.forEach(student => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><b>${student.name}</b></td>
                <td>#${student.rollNo}</td>
                <td>${student.course}</td>
                <td>${student.attendanceCount} / ${this.totalSessions}</td>
                <td>
                    <div class="action-btns">
                        <button onclick="app.deleteStudent('${student.rollNo}')" class="btn btn-sm btn-outline" style="color: #f56565;">
                            <i data-lucide="trash-2"></i>
                        </button>
                    </div>
                </td>
            `;
            this.manageStudentListBody.appendChild(tr);
        });
        if (window.lucide) lucide.createIcons();
    }

    renderCharts() {
        const perfEl = document.getElementById('performanceChart');
        const courseEl = document.getElementById('courseChart');
        if (!perfEl || !courseEl) return;

        const ctxPerf = perfEl.getContext('2d');
        const ctxCourse = courseEl.getContext('2d');

        if (this.charts.perf) this.charts.perf.destroy();
        if (this.charts.course) this.charts.course.destroy();

        const topStudents = [...this.students].sort((a, b) => b.attendanceCount - a.attendanceCount).slice(0, 5);
        this.charts.perf = new Chart(ctxPerf, {
            type: 'bar',
            data: {
                labels: topStudents.map(s => s.name),
                datasets: [{
                    label: 'Attendance Sessions',
                    data: topStudents.map(s => s.attendanceCount),
                    backgroundColor: '#3d7b82',
                    borderRadius: 8
                }]
            },
            options: { responsiveness: true, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' } } } }
        });

        const courses = {};
        this.students.forEach(s => courses[s.course] = (courses[s.course] || 0) + 1);
        this.charts.course = new Chart(ctxCourse, {
            type: 'doughnut',
            data: {
                labels: Object.keys(courses),
                datasets: [{
                    data: Object.values(courses),
                    backgroundColor: ['#3d7b82', '#805ad5', '#d69e2e', '#48bb78', '#f56565'],
                    borderWidth: 0
                }]
            },
            options: { responsiveness: true, plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8' } } } }
        });
    }

    getInitials(name) { return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2); }
}

const app = new AttendanceSystem();
