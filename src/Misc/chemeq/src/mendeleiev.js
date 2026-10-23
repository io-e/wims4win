
/* Script to enhance the interactivity of the periodic table */

function add_interaction(){
    var elements = jQuery("div.element");
    elements.each(function(i, elt){
	elt = jQuery(elt);
	elt.css("cursor", "help");
	var symbol = elt.find(".symbol").text();
	var Z = elt.find(".Z").text();
	var mass = elt.find(".mass").text();
	var struct = elt.find(".str0").html();
	elt.click(
	    function make_callback(o){
		return function (evt){
		    var x = elt.css("top"), y = elt.css("top");
		    o.html(""); /* erase all previous contents */
		    var p;
		    p = jQuery("<p>").text("Element: " + symbol);
		    o.append(p);
		    p = jQuery("<p>").text("Atomic number: " + Z);
		    o.append(p);
		    p = jQuery("<p>").text("Molar mass: " + mass);
		    o.append(p);
		    p = jQuery("<p>").html("Electronic structure: " + struct);
		    o.append(p);
		    o.dialog();
		    var top = evt.clientY;
		    if (top > 250){
			/* the element is low enough, let us animate above */
			top -= 250;
		    } else {
			top += 60;
		    }
		    var left = evt.clientX;
		    if (left > 340){
			/* the element is right enough, let us go to the left */
			left -= 340;
		    } else{
			left +=50;
		    }	       
		    o.parents(".ui-dialog").animate(
			{top: top+"px", left: left+"px"});
		}
	    }(jQuery("#one_element")));
    });
    jQuery("#wait").fadeOut();
}

function jQueryIsHere(){
    result = true;
    try {
	if (! jQuery) result = false;
    }
    catch(e) {
	if(e.name == "ReferenceError") {
	    result = false;
	}
    }
    return result
}

function scriptLoader(scriptUrl){
    /* copied from https://stackoverflow.com/questions/538745/how-to-tell-if-a-script-tag-failed-to-load */
    return new Promise(function (res, rej) {
	let script = document.createElement('script');
	script.src = scriptUrl;
	script.type = 'text/javascript';
	script.onerror = rej;
	script.async = true;
	script.onload = res;
	script.addEventListener('error',rej);
	script.addEventListener('load',res);
	document.head.appendChild(script);
    })    
}

function cssLoader(cssUrl){
    return new Promise(function (res, rej) {
	let link = document.createElement('link');
	link.href = cssUrl;
	link.rel = 'stylesheet';
	link.type = 'text/css';
	link.onerror = rej;
	link.async = true;
	link.onload = res;
	link.addEventListener('error',rej);
	link.addEventListener('load',res);
	document.head.appendChild(link);
    })    
}

function tryLocal(){
    return Promise.all([
	scriptLoader("http://localhost/javascript/jquery/jquery.js"),
	scriptLoader("http://localhost/javascript/jquery-ui/jquery-ui.js"),
	cssLoader("http://localhost/javascript/jquery-ui/css/smoothness/jquery-ui.css")]);
}

function tryRemote(){
    return Promise.all([
	scriptLoader("https://code.jquery.com/jquery-1.11.3.js"),
	scriptLoader("https://code.jquery.com/ui/1.13.2/jquery-ui.js"),
	cssLoader("https://code.jquery.com/ui/1.13.2/themes/base/jquery-ui.css")
    ]);
}

window.onload = function(){
    let p1 = new Promise((res, rej) => {
	if (jQueryIsHere()) res("jQuery found immediately");
	else rej("no jQuery immediately")
    })
	.then((msg) => {
	    console.log(msg);
	    add_interaction();
	})
	.catch((msg) =>{
	    console.log(msg);
	    tryLocal().then((values) => {
		console.log(values, "local scripts and css files not found");
		add_interaction();
	    }).catch((reason) => {
		console.log(reason);
		tryRemote().then((values) => {
		    console.log(values);
		    add_interaction();
		}).catch((reason) => {
		    console.log(reason, "remote scripts and css files not found");
		});
	    });
	})
};
