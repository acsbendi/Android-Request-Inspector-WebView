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

// Headers passed as a Headers instance — exercises normalizeFetchHeaders()
function triggerFetchStringUrlWithHeadersInstance() {
    fetch('https://example.com/api/headers-instance', {
        method: 'GET',
        headers: new Headers({
            'X-From-Instance': 'instance-value'
        })
    });
}

// Headers passed as an array of [name, value] tuples — exercises normalizeFetchHeaders()
function triggerFetchStringUrlWithHeadersArray() {
    fetch('https://example.com/api/headers-array', {
        method: 'GET',
        headers: [
            ['X-From-Array', 'array-value']
        ]
    });
}

// No options object at all — exercises the arguments-scope fix: arguments[1] starts
// as undefined and is set to {} before fetchOptions captures it.
function triggerFetchStringUrlNoOptions() {
    fetch('https://example.com/api/no-options');
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
