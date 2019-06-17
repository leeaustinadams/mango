mango = (function() {
    return {
        allowDrop: function(event) {
            event.preventDefault();
        },
        drag: function(event) {
            event.dataTransfer.setData('src', event.target.getAttribute('src'));
        },
        drop: function(event) {
            event.preventDefault();
            var data = "![](" + event.dataTransfer.getData('src') + ")";
            var v = event.target.value;
            var start = event.target.selectionStart;
            var end = event.target.selectionEnd;
            event.target.value = v.slice(0, start).concat(data, v.slice(end));
        }
    };
})();
