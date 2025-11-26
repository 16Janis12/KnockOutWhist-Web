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

onEvent("ReceivedHandEvent", receiveHandEvent)
onEvent("GameStateChangeEvent", receiveGameStateChange)