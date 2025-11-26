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

    exchangeBody(content, title);
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
onEvent("ReceivedHandEvent", receiveHandEvent)
onEvent("GameStateChangeEvent", receiveGameStateChange)
onEvent("CardPlayedEvent", receiveCardPlayedEvent)