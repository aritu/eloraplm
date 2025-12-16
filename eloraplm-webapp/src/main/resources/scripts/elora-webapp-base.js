/* List Choices */

function updateChoiceId(event) {
    var value = createCleanId(event.target.value);
    var index = event.target.id.split("-").pop();
    // Get real id
    PFJQ("input[name$='choice-id-" + index + "']").val(value);
}

function createCleanId(label) {
    return label
        .toLowerCase()
        .replaceAll(" ", "-")
        .replace(new RegExp(/[àáâãäå]/g), "a")
        .replace(new RegExp(/ç/g), "c")
        .replace(new RegExp(/[èéêë]/g), "e")
        .replace(new RegExp(/[ìíîï]/g), "i")
        .replace(new RegExp(/ñ/g), "n")
        .replace(new RegExp(/[òóôõö]/g), "o")
        .replace(new RegExp(/[ùúûü]/g), "u")
        .replace(/[^a-z0-9\-]/gi, "")
        .substring(0, 35);
}

PFJQ(document).ready(function () {
    PFJQ("body").on("change", "select.percentageSelect", function () {
        switch (PFJQ(this).val()) {
            case "0":
                PFJQ(this).removeAttr("class");
                PFJQ(this).addClass("percentageSelect");
                PFJQ(this).addClass("lowest");
                break;
            case "10":
            case "20":
            case "30":
                PFJQ(this).removeAttr("class");
                PFJQ(this).addClass("percentageSelect");
                PFJQ(this).addClass("low");
                break;
            case "40":
            case "50":
            case "60":
                PFJQ(this).removeAttr("class");
                PFJQ(this).addClass("percentageSelect");
                PFJQ(this).addClass("medium");
                break;
            case "70":
            case "80":
            case "90":
                PFJQ(this).removeAttr("class");
                PFJQ(this).addClass("percentageSelect");
                PFJQ(this).addClass("high");
                break;
            case "100":
                PFJQ(this).removeAttr("class");
                PFJQ(this).addClass("percentageSelect");
                PFJQ(this).addClass("highest");
                break;
        }

        PFJQ(this).blur();
    });
});

/* Copy text to clipboard*/

function fallbackCopyTextToClipboard(text) {
    var textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed"; //avoid scrolling to bottom
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    document.execCommand("copy");

    document.body.removeChild(textArea);
}

function copyTextToClipboard(text) {
    if (!navigator.clipboard) {
        fallbackCopyTextToClipboard(text);
        return;
    }
    navigator.clipboard.writeText(text);
}

var loadingButton;
function onStartLoadingButton(caller) {
    loadingButton = PFJQ('[id="' + caller + '"]');
    PFJQ(loadingButton).addClass("loading");
    PFJQ(loadingButton)
        .find("img")
        .attr("src", "/nuxeo/icons/anim_spinner_16.png");
    PFJQ(loadingButton).prop("disabled", true);
}

function onCompleteLoadingButton() {
    PFJQ(loadingButton).removeClass("loading");
    PFJQ(loadingButton).prop("disabled", false);
    PFJQ(loadingButton).find("img").attr("src", "/nuxeo/icons/reload_16.png");
    PFJQ(loadingButton).blur();
}

/* function onStartLoadingPfButton(caller) {
    loadingButton = PFJQ('[id="' + caller + '"]');
    var icon = loadingButton.children().get(0);
    PFJQ(icon).removeClass("pi-refresh");
    PFJQ(icon).addClass("pi-spin");
    PFJQ(icon).addClass("pi-spinner");
    PFJQ(loadingButton).prop("disabled", true);
}

function onCompleteLoadingPfButton() {
    var icon = loadingButton.children().get(0);
    PFJQ(icon).removeClass("pi-spinner");
    PFJQ(icon).removeClass("pi-spin");
    PFJQ(icon).addClass("pi-refresh");
    PFJQ(loadingButton).prop("disabled", false);
    PFJQ(loadingButton).blur();
} */
