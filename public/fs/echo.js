  var secureCb;
  var secureCbLabel;
  var wsUri;
  var consoleLog;
  var connectBut;
  var disconnectBut;
  var sendMessage;
  var sendBut;
  var clearLogBut;

  function echoHandlePageLoad()
  {
    secureCb = document.getElementById('secureCb');
    secureCb.checked = false;
    secureCb.onclick = toggleTlS;

    secureCbLabel = document.getElementById('secureCbLabel')

    wsUri = document.getElementById('wsUri');
    initializeLocation();

    // Connect if the user presses enter in the connect field.
    wsUri.onkeypress = function(e){
      if (!e) {
        e = window.event;
      }
      var keyCode = e.keyCode || e.which;
      if (keyCode == '13'){
        doConnect();
        return false;
      }
    }

    connectBut = document.getElementById('connect');
    connectBut.onclick = doConnect;

    disconnectBut = document.getElementById('disconnect');
    disconnectBut.onclick = doDisconnect;

    sendMessage = document.getElementById('sendMessage');

    // Send message if the user presses enter in the the sendMessage field.
    sendMessage.onkeypress = function(e){
      if (!e) {
        e = window.event;
      }
      var keyCode = e.keyCode || e.which;
      if (keyCode == '13'){
        doSend();
        return false;
      }
    }

    sendBut = document.getElementById('send');
    sendBut.onclick = doSend;

    consoleLog = document.getElementById('consoleLog');

    clearLogBut = document.getElementById('clearLogBut');
    clearLogBut.onclick = clearLog;

    setGuiConnected(false);

    document.getElementById('disconnect').onclick = doDisconnect;
    document.getElementById('send').onclick = doSend;

  }

  function initializeLocation() {
    var wsScheme = 'ws:';
    if (window.location.protocol.toString() == 'https:') {
      wsScheme = 'wss:';
      secureCb.checked = true;
    }
    wsUri.value = wsScheme+'//'+location.host+'/api/chatroom/socket/test'
  }

  function toggleTlS()
  {
    if (secureCb.checked)
    {
      wsUri.value = wsUri.value.replace('ws:', 'wss:');
    }
    else
    {
      wsUri.value = wsUri.value.replace ('wss:', 'ws:');
    }
  }

  function doConnect()
  {
    if (window.MozWebSocket)
    {
      logErrorToConsole('Info', 'This browser supports WebSocket using the MozWebSocket constructor');
      window.WebSocket = window.MozWebSocket;
    }
    else if (!window.WebSocket)
    {
      logErrorToConsole('ERROR', 'This browser does not have support for WebSocket');
      return;
    }

    // prefer text messages
    var uri = wsUri.value;
    websocket = new WebSocket(uri);
    websocket.onopen = function(evt) { onOpen(evt) };
    websocket.onclose = function(evt) { onClose(evt) };
    websocket.onmessage = function(evt) { onMessage(evt) };
    websocket.onerror = function(evt) { onError(evt) };
  }

  function doDisconnect()
  {
    websocket.close()
  }

  function doSend()
  {
    logTextToConsole('SENT: ' + sendMessage.value);
    websocket.send(sendMessage.value);
  }

  function logTextToConsole(text) {
    var span = document.createTextNode(text);
    logElementToConsole(span);
  }

  // label is a string like 'Info' or 'Error'.
  function logErrorToConsole(label, text) {
    var span = document.createElement('span');
    span.style.wordWrap = 'break-word';
    span.style.color = 'red';
    span.innerHTML = '<strong>'+label+':</strong> ';

    var text = document.createTextNode(text);
    span.appendChild(text);

    logElementToConsole(span);
  }

  function logElementToConsole(element) {

    var p = document.createElement('p');
    p.style.wordWrap = 'break-word';
    p.innerHTML = getSecureTag();
    p.appendChild(element);

    consoleLog.appendChild(p);

    while (consoleLog.childNodes.length > 50)
    {
      consoleLog.removeChild(consoleLog.firstChild);
    }

    consoleLog.scrollTop = consoleLog.scrollHeight;
  }

  function onOpen(evt)
  {
    logTextToConsole('CONNECTED');
    setGuiConnected(true);

    // For convenience, put the cursor in the message field, and at the end of the text.
    sendMessage.focus();
    sendMessage.selectionStart = sendMessage.selectionEnd = sendMessage.value.length;
  }

  function onClose(evt)
  {
    logTextToConsole('DISCONNECTED');
    setGuiConnected(false);
  }

  function onMessage(evt)
  {
    var span = document.createElement('span');
    span.style.wordWrap = 'break-word';
    span.style.color = 'blue';
    span.innerHTML = 'RECEIVED: ';

    var message = document.createTextNode(evt.data);
    span.appendChild(message);

    logElementToConsole(span);
  }

  function onError(evt)
  {
    logErrorToConsole('ERROR', evt.data);
  }

  function setGuiConnected(isConnected)
  {
    wsUri.disabled = isConnected;
    connectBut.disabled = isConnected;
    disconnectBut.disabled = !isConnected;
    sendMessage.disabled = !isConnected;
    sendBut.disabled = !isConnected;
    secureCb.disabled = isConnected;
    var labelColor = 'black';
    if (isConnected)
    {
      labelColor = '#999999';
    }
    secureCbLabel.style.color = labelColor;

  }

  function clearLog()
  {
    while (consoleLog.childNodes.length > 0)
    {
     consoleLog.removeChild(consoleLog.lastChild);
    }
  }

  function getSecureTag()
  {
    if (secureCb.checked)
    {
      return '<img src="img/tls-lock.png" width="6px" height="9px"> ';
    }
    else
    {
      return '';
    }
  }

window.addEventListener('load', echoHandlePageLoad, false);
