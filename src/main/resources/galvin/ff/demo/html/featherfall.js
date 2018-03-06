function FeatherfallImp(){
    var instance = this
    instance.loginToken = null

    var _debug = true
    var _loginForm = false
    var _loggedInAsForm = false

    function debug(message){
        if(_debug) console.log(message)
    }

    function noop(){}

    function getByIdOrThrow( id ){
        var result = document.getElementById(id)
        if(!!!result) throw "Unable to get element #" + id + "from DOM"
        return result
    }

    function setDisplay(elementId, display){
        var form = document.getElementById(elementId)
        if( !!form ){
            form.style.display = display
        }
    }

    function ajax(url, method){
        if(!!!method) method = "POST"

        var xhttp = new XMLHttpRequest();
        xhttp.open( method, url, true);
        xhttp.setRequestHeader('Content-Type', 'application/json')

        if( !!instance.loginToken ){
            xhttp.setRequestHeader('X-Auth-Token', instance.loginToken.uuid)

        }

        return xhttp
    }

    function isFunction(functionToCheck) {
        return !!functionToCheck && {}.toString.call(functionToCheck) === '[object Function]';
    }

    function createLoginForm( parentElementId ){
        if( !!!parentElementId ){
            parentElementId = "featherfall-login-parent"
            var loginParent = document.createElement("div")
            loginParent.setAttribute("id", parentElementId)
            document.body.appendChild(loginParent)
        }

        var span = document.createElement("span")
        span.innerHTML =
        "<div id='featherfall-login-form'>" +
        "<div id='featherfall-login-form-banner'></div>" +
        "<div id='featherfall-login-username-div'><input type='text' id='featherfall-login-username' placeholder='User name' value='admin'></div>"+
        "<div id='featherfall-login-password-div'><input type='password' id='featherfall-login-password' placeholder='Password' value='password'></div>"+
        "<div id='featherfall-login-submit-div'><input type='submit' id='submit' onclick='Featherfall.login()'></div>" +
        "</div>"

        var parent = getByIdOrThrow(parentElementId)
        parent.appendChild(span)
    }

    function hideLoginForm(){
        setDisplay("featherfall-login-form", "none")
    }

    function showLoginForm(){
        if( !_loginForm ){
            createLoginForm()
            _loginForm = true
        }
        setDisplay("featherfall-login-form", "")
    }

    function login(){
        var username = getByIdOrThrow("featherfall-login-username").value
        var password = getByIdOrThrow("featherfall-login-password").value

        var credentials = JSON.stringify( {
                    "username": username,
                    "password": password
        } )

        var xhttp = ajax("/api/login/")
        xhttp.onreadystatechange = function(response){
            if( xhttp.readyState == XMLHttpRequest.DONE && xhttp.status == 200 ) {
                instance.loginToken = JSON.parse(xhttp.responseText)
                if( isFunction(instance.onLogin) ){
                    instance.onLogin()
                }
            }
        }
        xhttp.send( credentials )
    }

    function createLoggedInAsForm( parentElementId ){
        if( !!!parentElementId ){
            parentElementId = "featherfall-logged-in-as-parent"
            var loginParent = document.createElement("div")
            loginParent.setAttribute("id", parentElementId)
            document.body.appendChild(loginParent)
        }

        var span = document.createElement("span")
        span.innerHTML =
        "<div id='featherfall-logged-in-as-form'>" +
        "<div id='featherfall-logged-in-as-banner'></div>" +
        "<div id='featherfall-logged-in-as-username-div' style='display:inline'>Logged in as: <span id='featherfall-logged-in-as-username-value-span'>" + instance.loginToken.user.login + "</span></div>"+
        " " +
        "<div id='featherfall-logged-in-as-logout-div' style='display:inline'><a href='#' onclick='Featherfall.logout(); return false;'>Logout</a></div>" +
        "</div>"

        var parent = getByIdOrThrow(parentElementId)
        parent.appendChild(span)
    }

    function hideLoggedInAsForm(){
        setDisplay("featherfall-logged-in-as-form", "none")
    }
    
    function showLoggedInAsForm(){
        if(!_loggedInAsForm){
            createLoggedInAsForm()
            _loggedInAsForm = true
        }

        var usernameSpan = getByIdOrThrow('featherfall-logged-in-as-username-value-span')
        usernameSpan.innerHTML = instance.loginToken.user.login

        setDisplay("featherfall-logged-in-as-form", "")
    }

    function logout(){
        if( !!Featherfall.loginToken ){
            var xhttp = ajax("/api/logout/")
            xhttp.onreadystatechange = function(response){
                if( xhttp.readyState == XMLHttpRequest.DONE && xhttp.status == 200 ) {
                    instance.loginToken = null
                    if( isFunction(instance.onLogout) ){
                        instance.onLogout()
                    }
                }
            }
            xhttp.send()
        }
    }


    instance.hideLoginForm = hideLoginForm
    instance.showLoginForm = showLoginForm
    instance.login = login

    instance.hideLoggedInAsForm = hideLoggedInAsForm
    instance.showLoggedInAsForm = showLoggedInAsForm
    instance.logout = logout

    instance.onLogin = noop
    instance.onLogout = noop
}

var Featherfall = new FeatherfallImp()