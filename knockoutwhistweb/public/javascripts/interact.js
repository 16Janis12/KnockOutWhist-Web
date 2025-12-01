function handlePlayCard(card, dog) {

    if(!canPlayCard) return
    canPlayCard = false;

    const cardId = card.dataset.cardId;

    console.debug(`Playing card ${cardId} from hand`)

    const wiggleKeyframes = [
        { transform: 'translateX(0)' },
        { transform: 'translateX(-5px)' },
        { transform: 'translateX(5px)' },
        { transform: 'translateX(-5px)' },
        { transform: 'translateX(0)' }
    ];

    const wiggleTiming = {
        duration: 400,
        iterations: 1,
        easing: 'ease-in-out',
        fill: 'forwards'
    };


    const cardslide = $('#card-slide')

    const payload = {
        cardindex: cardId,
        isDog: dog
    }
    sendEventAndWait("PlayCard", payload).then(
        () => {
            card.parentElement.remove();
            cardslide.find('.handcard').each(function(newIndex) {

                const $innerButton = $(this).find('.btn');
                $innerButton.attr('data-card-id', newIndex);

                const isInDogLife = $innerButton.attr('onclick').includes("'true'") ? 'true' : 'false';
                $innerButton.attr('onclick', `handlePlayCard(this, '${isInDogLife}')`);

                console.debug(`Re-indexed card: Old index was ${$innerButton.attr('data-card-id')}, New index is ${newIndex}`);
            });

            cardslide.addClass("inactive")
        }
    ).catch(
        (err) => {
            canPlayCard = true;
            const cardslide = $('#card-slide')
            cardslide.removeClass("inactive")
            card.parentElement.animate(wiggleKeyframes, wiggleTiming);
            alertMessage(err.message)
        }
    )
}

function handleSkipDogLife(button) {
    // TODO needs implementation
}
function startGame() {
    sendEvent("StartGame")
}

function handleTrumpSelection(object) {
    const $button = $(object);
    const trumpIndex = parseInt($button.data('trump'));
    const payload = {
        suitIndex: trumpIndex
    }
    sendEvent("PickTrumpsuit", payload)

}
function handleKickPlayer(playerId) {
    sendEvent("KickPlayer", {
        playerId: playerId
    })
}
