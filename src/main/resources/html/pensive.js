/**
 * pensive.js
 *
 * Author: Tom Parker
 * 
 *
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

/* ------------------------------------------------------------------------- */

/** time span of a single image in ms. */
var refreshPeriodMs = 1000 * +'${refreshPeriod}';

var DAY_MS = 24 * 60 * 60 * 1000;

/** format used to display time tags */
var timeFormatter = new SimpleDateFormat("HH:mm");

/** format used to display long time string at top of page */
var dateFormatter = new SimpleDateFormat("d MMMM, yyyy");

/** format used to display initial date in absolutre time box */
var ATDateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

/** format used to form path to images. Passed in from config file */
var pathFormatter = new SimpleDateFormat("${filePathFormat}");

/** format used to form image suffix. Passed in from config file */
var fileFormatter = new SimpleDateFormat("${fileSuffixFormat}");

/** end time */
var mosaicEnd;

var cellEnd;

/** start time displayed */
var startTime;

/** end time displayed */
var endTime;

/** span of a single mosaic row in ms. */
var rowSpanMs;

/** span of a mosaic in ms */
var mosaicSpanMs;

var mode;

var dailyMosaic;

var mosaicPosition;

var ATDateFormat = /^((19|20)\d{2})[-/]?(1[0-2]|0\d)[-/]?([0-2]\d|3[0-1])(T|\s*)+([01]\d|2[0-3]):?([0-5]\d)$/;

/**
 * Handle all initialization stuff here.
 */
function init() {
	mode = "mosaic";
	mosaicPosition = 0;
	mosaicEnd = new Date(getMostRecentEnd());
	cellEnd = new Date(getMostRecentEnd());
	rowSpanMs = hToMs(1);
	mosaicSpanMs = hToMs(3);
	endTime = new Date();
	startTime = new Date();
	if ($("#network option").size() == 1) {
		$("#network").hide();
	}
	
	parseParameters();
	registerEventHandlers();
	initalizeDialogs();

	// leanModal init
	$('form').submit(function(e) {
		return false;
	});
	$("a[rel*='leanModal']").leanModal({
		top : 110,
		overlay : 0.75,
		closeButton : ".hidemodal"
	});

	// fire subnet change trigger to get things rolling
	<#if selectedNetwork??>
		$("#network").val("${selectedNetwork}").prop('selected', true);
	</#if>
	$("#network").trigger("change");
	
}

function initalizeDialogs() {
	// Absolute date
	$("#ATDate").val(ATDateFormatter.formatUTC(startTime));
	$("#ATDate").addClass("valid");

	// Mosaic options
	dailyMosaic = $("#dailyMosaic").not(':checked');

}

function getMostRecentEnd() {
	var et = Date.now();
	return (et - (et % (refreshPeriodMs)));
}

function updateMainFrame(e) {
	if (e != null) {
		if (e.data.mode != null)
			mode = e.data.mode;
		if (e.data.cellEnd != null)
			cellEnd = e.data.cellEnd;
	}

	if (mode == "singlePlot") {
		mosaicPosition = $(document).scrollTop();
		displayPlot();
	} else {
		displayMosaic();
	}
}
/**
 * Parse request parameters. Only used when page is first loaded.
 */
function parseParameters() {
	var param;
	var network;

	param = getUrlParameter("mode");
	if (param != null)
		mode = param;

	param = getUrlParameter("network");
	if (param != null) {
		network = decodeURIComponent(param);
		$("#network").val(network).prop('selected', true);
	}

	param = getUrlParameter("subnet");
	if (param != null)
		$("#" + network + "Subnets").val(decodeURIComponent(param)).prop(
				'selected', true);

	param = getUrlParameter("cellEndM");
	if (param != null)
		cellEnd = new Date(mToMs(param));

	param = getUrlParameter("mosaicEndM");
	if (param != null)
		mosaicEnd = new Date(mToMs(param));

	/** a single URL parameter */
	param = getUrlParameter("rowSpanM");
	if ($.isNumeric(param))
		rowSpanMs = mToMs(param);

	param = getUrlParameter("mosaicSpanH");
	if ($.isNumeric(param))
		mosaicSpanMs = hToMs(param);

}

/**
 * register my event handlers.
 */
function registerEventHandlers() {
	$("#nextSubnet").on('click', {
		step : 1
	}, incrementSubnet);
	$("#perviousSubnet").on('click', {
		step : -1
	}, incrementSubnet);
	$("#currentImage").on('click', {
		step : (Math.pow(2, 32) - 1)
	}, incrementTime);
	$("#nextImage").on('click', {
		step : 1
	}, incrementTime);
	$("#previousImage").on('click', {
		step : -1
	}, incrementTime);
	$("#mosaicButton").on('click', {
		mode : "mosaic"
	}, updateMainFrame);
	$(".subnet").on('change', updateSubnet);
	$("#network").on('change', updateNetwork);
	$("#permalinkButton").on('click', populatePermalink);
	$(".positiveInt").on('keyup', function() {
		this.value = this.value.replace(/[^0-9]/g, '');
	});
	$("#ATDate").on('keyup', validateATInput);
	$("#dailyMosaic").on('click', updateDailyMosaic);
}

function validateATInput() {
	var inputField = $("#ATDate");
	var input = inputField.val();

	// var valid = dateFormats.some(function(regEx){return regEx.test(input)});

	var valid = ATDateFormat.test(input);

	if (valid)
		inputField.removeClass("invalid");
	else
		inputField.addClass("invalid");

	$("#absoluteTimeBtn").prop("disabled", !valid);
}

function updateDailyMosaic() {
	dailyMosaic = $("#dailyMosaic").is(":checked");
	$("#mosaicSpanH").prop("disabled", dailyMosaic);
}

function populatePermalink() {
	var URL = window.location.href

	var network = $("#network option:selected").text();
	var subnet = $('#' + network + 'Subnets option:selected').text();

	var q = URL.indexOf('?');
	if (q != -1)
		URL = URL.substring(0, q);

	URL += "?mode=" + mode;
	URL += "&network=" + encodeURIComponent(network);
	URL += "&subnet=" + encodeURIComponent(subnet);

	if (mode == "singlePlot") {
		URL += "&cellEndM=" + msToM(cellEnd.getTime());

	} else {
		URL += "&mosaicEndM=" + msToM(mosaicEnd.getTime());
		URL += "&rowSpanM=" + msToM(rowSpanMs);
		URL += "&mosaicSpanH=" + msToH(mosaicSpanMs);
	}

	var link = $(document.createElement('a'));
	$("#permalinkURL").empty();
	$("#permalinkURL").append(link);
	link.attr('href', URL);
	link.addClass('permalink');
	link.text(URL);
}

/* The subnet changed, now what? */
function updateSubnet() {
	var network = $("#network option:selected").text();
	var subnet = $('#' + network + 'Subnets option:selected').text();
	$("#subnetName").text(subnet);
	$(document).prop('title', subnet + ' - Pensive');
	updateMainFrame();
}

function updateNetwork() {
	var network = $("#network option:selected").text();
	$('.subnet:not(#' + network + 'Subnets)').hide();
	$('#' + network + 'Subnets').show();

	$('#' + network + 'Subnets').trigger("change");
}

function updateMosaicOptions() {
	var spanMS;
	var now = getMostRecentEnd();

	if ($("#dailyMosaic").is(":checked")) {
		now += DAY_MS - (now % DAY_MS);
		mosaicEnd = new Date(now);
		spanMs = hToMs(24);
	} else {
		if (mosaicEnd.getTime() > now)
			mosaicEnd = new Date(now);

		spanMs = hToMs($("#mosaicSpanH").val());
	}

	mosaicSpanMs = spanMs;

	spanMs = mToMs($("#mosaicRowSpanM").val());
	rowSpanMs = refreshPeriodMs * Math.ceil(spanMs / refreshPeriodMs);

	updateMainFrame();
}

function updateAbsoluteTime() {
	var date = $("#ATDate").val();
	date = date.replace(/(-\d{2}) (\d{2}:)/, '$1T$2');

	date = date.replace(ATDateFormat, '$1-$3-$4T$6:$7+00:00');

	dateMs = new Date(date).getTime();
	dateMs -= dateMs % refreshPeriodMs;

	if (mode == "singlePlot") {
		var end = Math.min(getMostRecentEnd(), dateMs + refreshPeriodMs);
		cellEnd = new Date(end);
	} else {
		var end = Math.min(getMostRecentEnd(), dateMs + mosaicSpanMs);
		mosaicEnd = new Date(end);
	}
	updateMainFrame();
}

/* move selected subnet up or down */
function incrementSubnet(e) {
	var network = $("#network option:selected").text();
	var subnetSelector = '#' + network + 'Subnets';
	var step = e.data.step;
	var idx = $(subnetSelector).prop("selectedIndex");
	var count = $(subnetSelector + ' option').length;
	$(subnetSelector).prop("selectedIndex", (idx + step + count) % count);
	$(subnetSelector).trigger("change");
}

/* The time changed, now what? */
function updateTimeLabel() {
	$("#timeSpan").text(
			dateFormatter.formatUTC(startTime) + " "
					+ timeFormatter.formatUTC(startTime) + " - "
					+ timeFormatter.formatUTC(endTime) + " UTC");
}

function incrementTime(e) {
	var step = e.data.step;
	var now = getMostRecentEnd();

	if (mode == "singlePlot") {
		var newEndMs = cellEnd.getTime() + (step * refreshPeriodMs);

		if (newEndMs <= now)
			cellEnd.setTime(newEndMs);
		else
			cellEnd.setTime(now);
	} else {
		var newEndMs = mosaicEnd.getTime() + (step * mosaicSpanMs);

		if (newEndMs <= now)
			mosaicEnd.setTime(newEndMs);
		else
			mosaicEnd.setTime(now);

		if ($("#dailyMosaic").is(":checked")) {
			var newEndMs = mosaicEnd.getTime();
			if (newEndMs == now)
				newEndMs += DAY_MS;

			newEndMs -= newEndMs % DAY_MS;
			mosaicEnd = new Date(newEndMs);

		}
	}
	updateMainFrame();
}

function displayMosaic() {
	$("#mosaicButton").hide();
	$("#mosaicOptionsButton").show();
	mode = "mosaic";

	var network = $("#network option:selected").text();
	var subnet = $('#' + network + 'Subnets option:selected').text();

	var frame = $("#mainFrame");
	frame.empty();

	var mosaicEndMs = mosaicEnd.getTime();
	var mosaicStartMs = mosaicEndMs - mosaicSpanMs;
	startTime.setTime(mosaicStartMs);
	endTime.setTime(mosaicEndMs);
	updateTimeLabel();
	var table = $(document.createElement('table'));
	frame.append(table);
	table.addClass('center');

	var rowStartMs = mosaicStartMs;
	while (rowStartMs < mosaicEndMs) {
		var cell;

		var rowStart = new Date(rowStartMs);
		var rowEndMs = rowStartMs + rowSpanMs;

		var row = $(document.createElement('tr'));
		table.append(row);
		row.addClass("mosaic");

		cell = $(document.createElement('td'));
		row.append(cell);
		cell.addClass("mosaicTitle");
		cell.html(timeFormatter.formatUTC(rowStart)
				+ " <span class=\"small\">UTC</span>");

		var cellEndMs = rowStart.getTime() + refreshPeriodMs;
		while (cellEndMs <= rowEndMs) {
			cell = $(document.createElement('td'));
			row.append(cell);
			cell.addClass("mosaic");

			var cellEnd = new Date(cellEndMs);
			var url = network + "/" + subnet + "/"
					+ pathFormatter.formatUTC(cellEnd) + "/" + subnet
					+ fileFormatter.formatUTC(cellEnd) + "_thumb.png";

			var image = $(document.createElement('img'));
			cell.append(image);
			image.addClass("mosaic");
			image.attr('src', url);
			image.on('click', {
				mode : "singlePlot",
				cellEnd : cellEnd
			}, updateMainFrame);
			image.on('error', imageNotFound);

			cellEndMs += refreshPeriodMs;
		}

		cell = $(document.createElement('td'));
		row.append(cell);
		cell.addClass("mosaicTitle");
		cell.html(timeFormatter.formatUTC(new Date(rowEndMs))
				+ " <span class=\"small\">UTC</span>");

		rowStartMs += rowSpanMs;
	}
	
	if (mosaicPosition != 0) {
		$(document).scrollTop(mosaicPosition);
		mosaicPosition = 0;
	}
}

function imageNotFound(e) {
	var URL = $(e.target).attr('src');
	var cell = $(e.target).parent();
	cell.empty();
	cell.removeClass("mosaic");
	cell.addClass("badImage");
	var link = $(document.createElement('a'));
	cell.append(link);
	link.attr('href', URL);
	link.html("Image not<br>available");
	link.addClass("badImage");
}

function displayPlot() {
	$("#mosaicButton").show();
	$("#mosaicOptionsButton").hide();

	endTime.setTime(cellEnd);
	startTime.setTime(cellEnd - refreshPeriodMs);
	updateTimeLabel();

	var frame = $("#mainFrame");
	frame.empty();

	var image = $(document.createElement('img'));
	frame.append(image);

	var network = $("#network option:selected").text();
	var subnet = $("#" + network + "Subnets option:selected").text();
	var url = network + "/" + subnet + "/" + pathFormatter.formatUTC(cellEnd)
			+ "/" + subnet + fileFormatter.formatUTC(cellEnd) + ".png";
	image.attr('src', url);
}
