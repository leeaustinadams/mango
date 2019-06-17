mango = (function() {
    return {
        onload: function() {
            var form = document.getElementById('upload-form');
            var fileSelect = document.getElementById('file-select');
            var uploadStatus = document.getElementById('upload-status');
            var antiForgery = document.getElementById('anti-forgery-token');
            var articleId = document.getElementById('article-id');
            var fileUpload = document.getElementById('file-upload');
            form.onsubmit = function(event) {
                var files = fileSelect.files;
                var formData = new FormData();
                var i;
                var file;
                var xhr;
                var mediaIds;
                event.preventDefault();
                fileUpload.innerHTML = 'Uploading...';
                for (i = 0; i < files.length; i++) {
                    file = files[i];
                    formData.append('files[]', file, file.name);
                }
                formData.append('__anti-forgery-token', antiForgery.value);
                formData.append('article-id', articleId.value);
                xhr = new XMLHttpRequest();
                xhr.onload = function(event) {
                    if (xhr.status === 200) {
                        fileUpload.innerHTML = 'Upload';
                        uploadStatus.innerHTML = 'Done';
                    } else {
                        uploadStatus.innerHTML = xhr.responseText;
                    }
                };
                xhr.onprogress = function(event) {
                    uploadStatus.innerHTML = event.loaded + '/' + event.total;
                };
                xhr.open(form.method, form.action, true);
                xhr.send(formData);
            }
        }
    };
})();

window.onload = function() { mango.onload(); };
