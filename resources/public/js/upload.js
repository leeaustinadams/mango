// License(s)
// Copyright (c) lee@4d4ms.com
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
// Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
