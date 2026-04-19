// =============================================
// PCNX 表单提交系统 - 前端交互逻辑
// =============================================

document.addEventListener('DOMContentLoaded', function () {

    // ===== 文件上传处理 =====
    const fileInput = document.getElementById('fileInput');
    const fileList = document.getElementById('fileList');
    const uploadArea = document.getElementById('uploadArea');

    if (fileInput && fileList && uploadArea) {
        let selectedFiles = [];

        // 点击上传
        fileInput.addEventListener('change', function () {
            selectedFiles = []
            addFiles(Array.from(this.files));
            // this.value = ''; // 重置input，允许重复选择同一文件
        });

        // 拖拽上传
        uploadArea.addEventListener('dragover', function (e) {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });
        uploadArea.addEventListener('dragleave', function () {
            uploadArea.classList.remove('dragover');
        });
        uploadArea.addEventListener('drop', function (e) {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            addFiles(Array.from(e.dataTransfer.files));
        });

        function addFiles(files) {
            files.forEach(file => {
                // 检查是否已存在
                const exists = selectedFiles.some(f => f.name === file.name && f.size === file.size);
                if (!exists) {
                    selectedFiles.push(file);
                }
            });
            renderFileList();
            syncFilesToInput();
        }

        function removeFile(index) {
            selectedFiles.splice(index, 1);
            renderFileList();
            syncFilesToInput();
        }

        function renderFileList() {
            fileList.innerHTML = '';
            selectedFiles.forEach((file, index) => {
                const item = document.createElement('div');
                item.className = 'file-item';
                item.innerHTML = `
                    <div class="file-item-name">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="color:#6366f1;flex-shrink:0">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                        </svg>
                        <span title="${escHtml(file.name)}">${escHtml(file.name)}</span>
                        <span style="color:#94a3b8;font-size:12px;flex-shrink:0">${formatSize(file.size)}</span>
                    </div>
                    <div class="file-remove" title="移除" onclick="removeFileById(${index})">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </div>
                `;
                fileList.appendChild(item);
            });
        }

        function syncFilesToInput() {
            const dt = new DataTransfer();
            selectedFiles.forEach(f => dt.items.add(f));
            fileInput.files = dt.files;
        }

        // 暴露到全局给 onclick 调用
        window.removeFileById = function (index) {
            removeFile(index);
        };

        function formatSize(bytes) {
            if (bytes < 1024) return bytes + ' B';
            if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
            return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
        }

        function escHtml(str) {
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }
    }

    // ===== 表单验证 =====
    const form = document.getElementById('submitForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            let valid = true;

            // 验证分类（至少选一个）
            const categoryChecks = form.querySelectorAll('input[name="categories"]');
            const categoryError = document.getElementById('categoryError');
            const anyChecked = Array.from(categoryChecks).some(c => c.checked);
            if (!anyChecked) {
                if (categoryError) { categoryError.classList.add('show'); }
                valid = false;
            } else {
                if (categoryError) { categoryError.classList.remove('show'); }
            }

            // 验证产品类型（必须选一个）
            const productRadios = form.querySelectorAll('input[name="productType"]');
            const productError = document.getElementById('productError');
            const productChecked = Array.from(productRadios).some(r => r.checked);
            if (!productChecked) {
                if (productError) { productError.classList.add('show'); }
                valid = false;
            } else {
                if (productError) { productError.classList.remove('show'); }
            }

            // 验证项目规模
            const scaleRadios = form.querySelectorAll('input[name="projectScale"]');
            const scaleError = document.getElementById('scaleError');
            const scaleChecked = Array.from(scaleRadios).some(r => r.checked);
            if (!scaleChecked) {
                if (scaleError) { scaleError.classList.add('show'); }
                valid = false;
            } else {
                if (scaleError) { scaleError.classList.remove('show'); }
            }

            // 验证留言（必填）
            const messageField = document.getElementById('message');
            const messageError = document.getElementById('messageError');
            if (messageField && messageField.value.trim() === '') {
                if (messageError) { messageError.classList.add('show'); }
                valid = false;
            } else {
                if (messageError) { messageError.classList.remove('show'); }
            }

            // 验证邮箱（必填）
            const emailField = document.getElementById('clientEmail');
            const emailError = document.getElementById('emailError');
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (emailField && (emailField.value.trim() === '' || !emailRegex.test(emailField.value.trim()))) {
                if (emailError) { emailError.classList.add('show'); }
                valid = false;
            } else {
                if (emailError) { emailError.classList.remove('show'); }
            }

            if (!valid) {
                e.preventDefault();
                // 滚动到第一个错误
                const firstError = form.querySelector('.field-error.show');
                if (firstError) {
                    firstError.closest('.form-group').scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
                return;
            }

            // 防重复提交
            const submitBtn = document.getElementById('submitBtn');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = `<span class="spinner"></span><span class="btn-text">提交中...</span>`;
            }
        });

        // 实时清除错误提示
        form.querySelectorAll('input[name="categories"]').forEach(c => {
            c.addEventListener('change', function () {
                const err = document.getElementById('categoryError');
                if (err) err.classList.remove('show');
            });
        });
        form.querySelectorAll('input[name="productType"]').forEach(r => {
            r.addEventListener('change', function () {
                const err = document.getElementById('productError');
                if (err) err.classList.remove('show');
            });
        });
        form.querySelectorAll('input[name="projectScale"]').forEach(r => {
            r.addEventListener('change', function () {
                const err = document.getElementById('scaleError');
                if (err) err.classList.remove('show');
            });
        });
        const msgField = document.getElementById('message');
        if (msgField) {
            msgField.addEventListener('input', function () {
                const err = document.getElementById('messageError');
                if (err && this.value.trim() !== '') err.classList.remove('show');
            });
        }
        const emailField = document.getElementById('clientEmail');
        if (emailField) {
            emailField.addEventListener('input', function () {
                const err = document.getElementById('emailError');
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (err && emailRegex.test(this.value.trim())) err.classList.remove('show');
            });
        }
    }

    // ===== 成功提示自动消失 =====
    const successAlert = document.querySelector('.alert-success');
    if (successAlert) {
        setTimeout(() => {
            successAlert.style.transition = 'opacity .4s ease';
            successAlert.style.opacity = '0';
            setTimeout(() => successAlert.remove(), 400);
        }, 5000);
    }

});
