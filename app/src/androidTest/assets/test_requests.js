function triggerFetchStringUrl() {
    fetch('https://example.com/api/data', {
        method: 'POST',
        body: '{"key":"value"}',
        headers: {
            'Content-Type': 'application/json',
            'X-Custom': 'test-value'
        }
    });
}

function triggerFetchRequestObject() {
    fetch(new Request('https://example.com/api/request', {
        method: 'PUT',
        headers: { 'X-Request-Header': 'request-value' }
    }));
}

function triggerXhr() {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'https://example.com/api/xhr', true);
    xhr.setRequestHeader('Content-Type', 'text/plain');
    xhr.setRequestHeader('X-Xhr-Header', 'xhr-value');
    xhr.send('xhr-body-content');
}

function submitFormGet() {
    document.getElementById('formGet').submit();
}

function submitFormPostUrlEncoded() {
    document.getElementById('formPostUrlEncoded').submit();
}

function submitFormPostMultipart() {
    document.getElementById('formPostMultipart').submit();
}

function submitFormPostPlainText() {
    document.getElementById('formPostPlainText').submit();
}
