const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ACCEPTED_TYPES = new Set(["image/jpeg", "image/png"]);

const imageInput = document.querySelector("#imageInput");
const dropZone = document.querySelector("#dropZone");
const previewWrap = document.querySelector("#previewWrap");
const previewImage = document.querySelector("#previewImage");
const fileName = document.querySelector("#fileName");
const clearButton = document.querySelector("#clearButton");
const recognizeButton = document.querySelector("#recognizeButton");
const message = document.querySelector("#message");
const serviceStatus = document.querySelector("#serviceStatus");
const semesterValue = document.querySelector("#semesterValue");
const weekValue = document.querySelector("#weekValue");
const courseCount = document.querySelector("#courseCount");
const courseTableBody = document.querySelector("#courseTableBody");
const jsonOutput = document.querySelector("#jsonOutput");

let selectedFile = null;
let previewUrl = null;

function setMessage(text, type = "") {
    message.textContent = text;
    message.className = `message ${type}`.trim();
}

function setStatus(text, type = "") {
    serviceStatus.textContent = text;
    serviceStatus.className = `service-chip ${type}`.trim();
}

function resetResult(clearJson = true) {
    semesterValue.textContent = "-";
    weekValue.textContent = "-";
    courseCount.textContent = "0";
    courseTableBody.innerHTML = '<tr><td colspan="6" class="empty-cell">暂无识别结果</td></tr>';

    if (clearJson) {
        jsonOutput.textContent = "{}";
    }
}

function clearSelection() {
    selectedFile = null;
    imageInput.value = "";
    fileName.textContent = "未选择文件";
    previewWrap.hidden = true;
    previewImage.removeAttribute("src");
    clearButton.disabled = true;
    recognizeButton.disabled = true;
    setMessage("");
    setStatus("等待图片");

    if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        previewUrl = null;
    }
}

function validateFile(file) {
    if (!file || file.size === 0) {
        return "图片文件不能为空";
    }

    if (!ACCEPTED_TYPES.has(file.type)) {
        return "仅支持 JPG、PNG 格式的图片";
    }

    if (file.size > MAX_FILE_SIZE) {
        return "图片大小不能超过 5MB";
    }

    return "";
}

function selectFile(file) {
    const error = validateFile(file);

    resetResult();

    if (error) {
        clearSelection();
        setMessage(error, "error");
        setStatus("文件无效", "error");
        return;
    }

    selectedFile = file;
    fileName.textContent = file.name;
    clearButton.disabled = false;
    recognizeButton.disabled = false;
    setMessage("");
    setStatus("已选择", "active");

    if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
    }

    previewUrl = URL.createObjectURL(file);
    previewImage.src = previewUrl;
    previewWrap.hidden = false;
}

function normalizeCourses(data) {
    if (!data || !Array.isArray(data.courses)) {
        return [];
    }

    return data.courses;
}

function renderCourses(courses) {
    if (courses.length === 0) {
        courseTableBody.innerHTML = '<tr><td colspan="6" class="empty-cell">暂无课程数据</td></tr>';
        return;
    }

    courseTableBody.innerHTML = courses.map((course) => {
        const start = course.startSection ?? "";
        const end = course.endSection ?? "";
        const section = start && end ? `${start}-${end}` : (start || end || "未知");

        return `
            <tr>
                <td>${escapeHtml(course.courseName || "未知")}</td>
                <td>${escapeHtml(formatDay(course.dayOfWeek))}</td>
                <td>${escapeHtml(section)}</td>
                <td>${escapeHtml(course.location || "未知")}</td>
                <td>${escapeHtml(course.teacher || "未知")}</td>
                <td>${escapeHtml(course.weekRange || "未知")}</td>
            </tr>
        `;
    }).join("");
}

function renderResult(payload) {
    const data = payload?.data || {};
    const courses = normalizeCourses(data);

    semesterValue.textContent = data.semester || "未知";
    weekValue.textContent = data.week || "未知";
    courseCount.textContent = String(courses.length);
    jsonOutput.textContent = JSON.stringify(payload, null, 2);
    renderCourses(courses);
}

function formatDay(value) {
    const dayMap = {
        1: "周一",
        2: "周二",
        3: "周三",
        4: "周四",
        5: "周五",
        6: "周六",
        7: "周日"
    };

    const key = String(value ?? "");
    return dayMap[key] || (key ? `周${key}` : "未知");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function recognizeSchedule() {
    const error = validateFile(selectedFile);

    if (error) {
        setMessage(error, "error");
        setStatus("文件无效", "error");
        return;
    }

    const formData = new FormData();
    formData.append("image", selectedFile);

    recognizeButton.disabled = true;
    recognizeButton.classList.add("loading");
    recognizeButton.textContent = "识别中";
    setMessage("正在提交图片", "");
    setStatus("识别中", "active");

    try {
        const response = await fetch("/api/ai/schedule/recognize", {
            method: "POST",
            body: formData
        });

        const contentType = response.headers.get("content-type") || "";
        const payload = contentType.includes("application/json")
            ? await response.json()
            : { code: response.status, message: await response.text(), data: null };

        jsonOutput.textContent = JSON.stringify(payload, null, 2);

        if (!response.ok || payload.code !== 200) {
            throw new Error(payload.message || `识别失败，状态码 ${response.status}`);
        }

        renderResult(payload);
        setMessage(payload.message || "识别完成", "success");
        setStatus("识别完成", "active");
    } catch (error) {
        resetResult(false);
        const fallbackMessage = error instanceof TypeError ? "无法连接识别服务" : error.message;
        setMessage(fallbackMessage || "无法连接识别服务", "error");
        setStatus("识别失败", "error");
    } finally {
        recognizeButton.disabled = !selectedFile;
        recognizeButton.classList.remove("loading");
        recognizeButton.textContent = "开始识别";
    }
}

imageInput.addEventListener("change", (event) => {
    selectFile(event.target.files[0]);
});

clearButton.addEventListener("click", () => {
    clearSelection();
    resetResult();
});

recognizeButton.addEventListener("click", recognizeSchedule);

dropZone.addEventListener("dragover", (event) => {
    event.preventDefault();
    dropZone.classList.add("drag-over");
});

dropZone.addEventListener("dragleave", () => {
    dropZone.classList.remove("drag-over");
});

dropZone.addEventListener("drop", (event) => {
    event.preventDefault();
    dropZone.classList.remove("drag-over");
    selectFile(event.dataTransfer.files[0]);
});
