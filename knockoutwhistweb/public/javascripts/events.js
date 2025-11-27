var canPlayCard = false;

function alertMessage(message) {
    let newHtml = '';
    const alertId = `alert-${Date.now()}`;
    const fadeTime = 500;
    const duration = 5000;
    newHtml += `
        <div class="fixed-top d-flex justify-content-center mt-3" style="z-index: 1050;">
        <div
        id="${alertId}" class="alert alert-primary d-flex align-items-center p-2 mb-0 w-auto" role="alert" style="display: none;">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" class="bi flex-shrink-0 me-2" viewBox="0 0 16 16" role="img" aria-label="Warning:">
                <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
            </svg>
            <div class="small">
                <small>${message}</small>
            </div>
        </div>
    </div>
    `;
    $('#main-body').prepend(newHtml);
    const $notice = $(`#${alertId}`);
    $notice.fadeIn(fadeTime);
    setTimeout(function() {
        $notice.fadeOut(fadeTime, function() {
            $(this).parent().remove();
        });
    }, duration);
}
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
        const idx = card.idx;
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
function newRoundEvent(eventData) {
    const trumpsuit = eventData.trumpsuit;
    const players = eventData.players;

    const tableElement = $('#score-table-body');


    let tablehtml = `
        <h4 class="fw-bold mb-3 text-black">Tricks Won</h4>

                        <div class="d-flex justify-content-between score-header pb-1">
                            <div style="width: 50%">PLAYER</div>
                            <div style="width: 50%">TRICKS</div>
                        </div>
    `;

    players.forEach(
        tablehtml += `
            <div class="d-flex justify-content-between score-row pt-1">
                                <div style="width: 50%" class="text-truncate">'${players}'</div>
                                <div style="width: 50%">
                                    0
                                </div>
                            </div>
        `
    );
    tableElement.html(tablehtml);

    const trumpsuitClass = $('#trumpsuit');
    trumpsuitClass.html(trumpsuit);

}
function trickEndEvent(eventData) {
    const winner = eventData.playerwon;
    const players = eventData.playersin;
    const tricklist = eventData.tricklist;

    let newHtml = '';

    let tricktable = $('#score-table-body');

    newHtml += `
        <h4 class="fw-bold mb-3 text-black">Tricks Won</h4>

                        <div class="d-flex justify-content-between score-header pb-1">
                            <div style="width: 50%">PLAYER</div>
                            <div style="width: 50%">TRICKS</div>
                        </div>
    `;
    let playercounts = new Map();

    players.forEach( player => {
            playercounts.set(player, 0)
    });
    tricklist.forEach( player => {
            if ( player !== "Trick in Progress" && playercounts.has(player)) {
                playercounts.set(player, playercounts.get(player) + 1)
            }
        }
    )
    const playerorder = players.sort((playerA, playerB) => {
        const countA = playercounts.get(playerA.name) || 0;
        const countB = playercounts.get(playerB.name) || 0;
        return countB - countA;
    });
    playerorder.forEach( player => {
        newHtml += `
            <div class="d-flex justify-content-between score-row pt-1">
                                    <div style="width: 50%" class="text-truncate">${player}</div>
                                    <div style="width: 50%">
                                    ${playercounts.get(player)}
                                    </div>
                                </div>
        `
    });
    tricktable.html(newHtml);
}
function newTrickEvent() {
    const firstCardContainer = $('#first-card-container');
    const emptyHtml = '';
    let newHtml = '';
    newHtml += `
        <img src="/assets/images/cards/1B.png" alt="Blank Card" width="80px" style="border-radius: 6px"/>
    `;
    firstCardContainer.html(newHtml);
    const playedCardsContainer = $('#trick-cards-container')
    playedCardsContainer.html(emptyHtml)
}
function requestCardEvent(eventData) {
    const player = eventData.player;
    const handElement = $('#card-slide')
    handElement.removeClass('inactive');
    canPlayCard = true;
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


function receiveTurnEvent(eventData) {
    const currentPlayer = eventData.currentPlayer;
    const nextPlayers = eventData.nextPlayers;

    const currentPlayerNameContainer = $('#current-player-name');
    const nextPlayersContainer = $('#next-players-container');
    const nextPlayerText = $('#next-players-text');

    let currentPlayerName = currentPlayer.name;
    if (currentPlayer.dog) {
        currentPlayerName += " 🐶";
    }
    currentPlayerNameContainer.text(currentPlayerName);

    if (nextPlayers.length === 0) {
        nextPlayerText.hide();
        nextPlayersContainer.html('');
    } else {
        console.log("Length"+nextPlayers.length);
        nextPlayerText.show();
        let nextPlayersHtml = '';
        nextPlayers.forEach((player) => {
            let playerName = player.name;
            if (player.dog) {
                playerName += " 🐶";
            }
            nextPlayersHtml += `<p class="fs-5 text-primary">${playerName}</p>`;
        });
        nextPlayersContainer.html(nextPlayersHtml);
    }
}

onEvent("ReceivedHandEvent", receiveHandEvent)
onEvent("GameStateChangeEvent", receiveGameStateChange)
onEvent("NewRoundEvent", newRoundEvent)
onEvent("TrickEndEvent", trickEndEvent)
onEvent("NewTrickEvent", newTrickEvent)
onEvent("RequestCardEvent", requestCardEvent)
onEvent("CardPlayedEvent", receiveCardPlayedEvent)
onEvent("LobbyUpdateEvent", receiveLobbyUpdateEvent)
onEvent("LeftEvent", receiveGameStateChange)
onEvent("KickEvent", receiveKickEvent)
onEvent("SessionClosed", receiveSessionClosedEvent)
onEvent("TurnEvent", receiveTurnEvent)

globalThis.alertMessage = alertMessage