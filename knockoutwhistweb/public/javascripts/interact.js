function handlePlayCard(cardIndex, dog) {
    const cardslide = $('#card-slide')
    cardslide.addClass("inactive")

    const payload = {
        cardindex: cardIndex,
        isDog: dog
    }
    sendEventAndWait("play Card", payload).then(
        () => {
            console.debug("play card successful")
            const datacardid = $(`#${cardIndex}`)
            datacardid.parent('.handcard').remove();
            cardslide.find('.handcard').each(function(newIndex) {

                const $innerButton = $(this).find('.btn');

                $innerButton.attr('id', newIndex);
                $innerButton.attr('data-card-id', newIndex);

                const isInDogLife = $innerButton.attr('onclick').includes("'true'") ? 'true' : 'false';
                $innerButton.attr('onclick', `handlePlayCard(${newIndex}, '${isInDogLife}')`);

                console.debug(`Re-indexed card: Old index was ${$innerButton.attr('data-card-id')}, New index is ${newIndex}`);
            });
        }
    ).catch(
        (err) => {
            const cardslide = $('#card-slide')
            console.warn("play card was not successful")
            if (err.message === "You can't play this card!") {
                cardslide.removeClass("inactive")
            }
            alertMessage("You aren't allowed to play this card")
        }
    )
}

function handleSkipDogLife(button) {
    // TODO needs implementation
}
function startGame() {
    sendEvent("Start Game")
}

function handleTrumpSelection(object) {
    const $button = $(object);
    const trumpIndex = parseInt($button.data('trump'));
    const payload = {
        suitIndex: trumpIndex
    }
    console.log("SENDING TRUMP SUIT SELECTION:", payload);
    sendEvent("Picked Trumpsuit", payload)

}
function handleKickPlayer(playerId) {
    // TODO needs implementation
}
