function parse() {
	var canvas = document.createElement('canvas');
	var ctx = canvas.getContext('2d');
	var FINGERPRINT = "D87E580A0F85F020E716C55418AADC82C4C37446";

	var img = new Image();
	img.onload = function() {
		canvas.width = img.width;
		canvas.height = img.height;
		ctx.drawImage(img, 0, 0, img.width, img.height);
		var webpage = "";
		ctx.getImageData(0, 0, img.width, img.height).data.forEach(
			function(item, index) {
				if ((index+1) % 4 == 0)
					return;
				webpage += String.fromCharCode(item);
			}
		);
		if (webpage.startsWith(FINGERPRINT)) {
			var json = JSON.parse(webpage.substr(FINGERPRINT.length));
			document.getElementsByTagName("html")[0].innerHTML = json["html"];
			document.title = json["title"];
		}
	}
	img.src = window.location;
}

parse();