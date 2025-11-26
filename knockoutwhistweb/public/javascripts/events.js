function receiveHandEvent(eventData) {
    //Data
    const dog = eventData.dog;
    const hand = eventData.hand;

    const handElement = $('#card-slide');
    handElement.addClass('ingame-cards-slide')

    let newHtml = '';

    //Build Hand Container
    hand.forEach((card) => {
        //Data
        const idx = card.idx
        const cardS = card.card;

        const cardHtml = `
                        <div class="col-auto handcard" style="border-radius: 6px">
                            <div class="btn btn-outline-light p-0 border-0 shadow-none" 
                                 data-card-id="${idx}" 
                                 style="border-radius: 6px" 
                                 onclick="handlePlayCard(this, '${dog}')">
                                
                                <img src="/assets/images/cards/${cardS}.png" width="120px" style="border-radius: 6px" alt="${cardS}"/>
                            </div>
                        </div>
                    `;
        newHtml += cardHtml;
    });

    //Build dog if needed
    if (dog) {
        newHtml += `
                        <div class="mt-2">
                            <button class="btn btn-danger" onclick="handleSkipDogLife(this)">Skip Turn</button>
                        </div>
                    `;
    }
    handElement.html(newHtml);
}

function receiveGameStateChange(eventData) {
    const content = eventData.content;
    const title = eventData.title || 'Knockout Whist';
    const url = eventData.url || null;

    exchangeBody(content, title, url);
}
function receiveCardPlayedEvent(eventData) {
    const firstCard = eventData.firstCard;
    const playedCards = eventData.playedCards;

    const trickCardsContainer = $('#trick-cards-container');
    const firstCardContainer = $('#first-card-container')

    let trickHTML = '';
    playedCards.forEach(cardCombo => {
        trickHTML += `
                            <div class="col-auto">
                                <div class="card text-center shadow-sm border-0 bg-transparent" style="width: 7rem; backdrop-filter: blur(4px);">
                                    <div class="p-2">
                                        <img src="/assets/images/cards/${cardCombo.cardId}.png" width="100%" alt="${cardCombo.cardId}"/>
                                    </div>
                                    <div class="card-body p-2 bg-transparent">
                                        <small class="fw-semibold text-secondary">${cardCombo.player}</small>
                                    </div>
                                </div>
                            </div>
                        `;
    });
    trickCardsContainer.html(trickHTML);

    let altText;
    let imageSrc;
    if (firstCard === "BLANK") {
        imageSrc = "/assets/images/cards/1B.png";
        altText = "Blank Card";
    } else {
        imageSrc = `/assets/images/cards/${firstCard}.png`;
        altText = `Card ${firstCard}`;
    }

    const newFirstCardHTML = `
                        <img src="${imageSrc}" alt="${altText}" width="80px" style="border-radius: 6px"/>
                    `;
    firstCardContainer.html(newFirstCardHTML);
}
function receiveLobbyUpdateEvent(eventData) {
    const host = eventData.host;
    const maxPlayers = eventData.maxPlayers;
    const players = eventData.players;

    const lobbyPlayersContainer = $('#players');
    const playerAmountBox = $('#playerAmount');

    let newHtml = ''

    if (host) {
        players.forEach(user => {

            const inner = user.self ? `<h5 class="card-title">${user.name} (You)</h5>
                                <a href="#" class="btn btn-danger disabled" aria-disabled="true" tabindex="-1">Remove</a>`
                : `    <h5 class="card-title">${user.name}</h5>
                                    <div class="btn btn-danger" onclick="removePlayer('${user.id}')">Remove</div>`

            newHtml += `<div class="col-auto my-auto m-3">
                            <div class="card" style="width: 18rem;">
                                <img src="/assets/images/profile.png" alt="Profile" class="card-img-top w-50 mx-auto mt-3" />
                                <div class="card-body">
                                    ${inner}
                                </div>
                            </div>
                        </div>`
        })
    } else {
        players.forEach(user => {

            const inner = user.self ? `<h5 class="card-title">${user.name} (You)</h5>` : `    <h5 class="card-title">${user.name}</h5>`

            newHtml += `<div class="col-auto my-auto m-3">
                            <div class="card" style="width: 18rem;">
                                <img src="/assets/images/profile.png" alt="Profile" class="card-img-top w-50 mx-auto mt-3" />
                                <div class="card-body">
                                    ${inner}
                                </div>
                            </div>
                        </div>`
        })
    }

    lobbyPlayersContainer.html(newHtml);
    playerAmountBox.text(`Players: ${players.length} / ${maxPlayers}`);

}
function receiveKickEvent(eventData) {
    $('#kickedModal').modal({
        backdrop: 'static',
        keyboard: false
    }).modal('show');

    setTimeout(() => {
        receiveGameStateChange(eventData)
    }, 5000);
}
function receiveSessionClosedEvent(eventData) {
    $('#sessionClosed').modal({
        backdrop: 'static',
        keyboard: false
    }).modal('show');

    setTimeout(() => {
        receiveGameStateChange(eventData)
    }, 5000);
}


onEvent("ReceivedHandEvent", receiveHandEvent)
onEvent("GameStateChangeEvent", receiveGameStateChange)
onEvent("CardPlayedEvent", receiveCardPlayedEvent)
onEvent("LobbyUpdateEvent", receiveLobbyUpdateEvent)
onEvent("LeftEvent", receiveGameStateChange)
onEvent("KickEvent", receiveKickEvent)
onEvent("SessionClosed", receiveSessionClosedEvent)
