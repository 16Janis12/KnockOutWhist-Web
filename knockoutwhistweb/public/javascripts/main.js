/*!
 * Color mode toggler for Bootstrap's docs (https://getbootstrap.com/)
 * Copyright 2011-2025 The Bootstrap Authors
 * Licensed under the Creative Commons Attribution 3.0 Unported License.
 */

(() => {
    'use strict'

    const getStoredTheme = () => localStorage.getItem('theme')
    const setStoredTheme = theme => localStorage.setItem('theme', theme)

    const getPreferredTheme = () => {
        const storedTheme = getStoredTheme()
        if (storedTheme) {
            return storedTheme
        }

        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }

    const setTheme = theme => {
        if (theme === 'auto') {
            document.documentElement.setAttribute('data-bs-theme', (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'))
        } else {
            document.documentElement.setAttribute('data-bs-theme', theme)
        }
    }

    setTheme(getPreferredTheme())

    const showActiveTheme = (theme, focus = false) => {
        const themeSwitcher = document.querySelector('#bd-theme')

        if (!themeSwitcher) {
            return
        }

        const themeSwitcherText = document.querySelector('#bd-theme-text')
        const activeThemeIcon = document.querySelector('.theme-icon-active use')
        const btnToActive = document.querySelector(`[data-bs-theme-value="${theme}"]`)
        const svgOfActiveBtn = btnToActive.querySelector('svg use').getAttribute('href')

        document.querySelectorAll('[data-bs-theme-value]').forEach(element => {
            element.classList.remove('active')
            element.setAttribute('aria-pressed', 'false')
        })

        btnToActive.classList.add('active')
        btnToActive.setAttribute('aria-pressed', 'true')
        activeThemeIcon.setAttribute('href', svgOfActiveBtn)
        const themeSwitcherLabel = `${themeSwitcherText.textContent} (${btnToActive.dataset.bsThemeValue})`
        themeSwitcher.setAttribute('aria-label', themeSwitcherLabel)

        if (focus) {
            themeSwitcher.focus()
        }
    }

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        const storedTheme = getStoredTheme()
        if (storedTheme !== 'light' && storedTheme !== 'dark') {
            setTheme(getPreferredTheme())
        }
    })

    window.addEventListener('DOMContentLoaded', () => {
        showActiveTheme(getPreferredTheme())

        document.querySelectorAll('[data-bs-theme-value]')
            .forEach(toggle => {
                toggle.addEventListener('click', () => {
                    const theme = toggle.getAttribute('data-bs-theme-value')
                    setStoredTheme(theme)
                    setTheme(theme)
                    showActiveTheme(theme, true)
                })
            })
    })
})()

function pollForUpdates(gameId) {
    console.log(`[DEBUG] Starting poll cycle for Game ID: ${gameId} at ${new Date().toISOString()}`);
    if (!gameId) {
        console.error("[DEBUG] Game ID is missing. Stopping poll.");
        return;
    }
    const $handElement = $('#card-slide');
    const $lobbyElement = $('#lobbybackground');
    const $mainmenuElement = $('#main-menu-screen')
    const $mainbody = $('#main-body')
    if (!$handElement.length && !$lobbyElement.length && !$mainmenuElement.length && !$mainbody.length) {
        setTimeout(() => pollForUpdates(gameId), 1000);
        return;
    }
    const route = jsRoutes.controllers.PollingController.polling(gameId);
    $.ajax({
        url: route.url,
        type: 'GET',
        dataType: 'json',

        success: (data => {
            if (!data) {
                console.log("[DEBUG] Received 204 No Content (Timeout). Restarting poll.");
                return;
            }
            if (data.status === "cardPlayed" && data.handData) {
                console.log("Event received: Card played. Redrawing hand.");
                const newHand = data.handData;
                let newHandHTML = '';
                $handElement.empty();

                if(data.animation) {
                        $handElement.addClass('ingame-cards-slide');
                } else {
                    $handElement.removeClass('ingame-cards-slide');
                }

                const dog = data.dog;

                newHand.forEach((cardId, index) => {
                    const cardHtml = `
                        <div class="col-auto handcard" style="border-radius: 6px">
                            <div class="btn btn-outline-light p-0 border-0 shadow-none" 
                                 data-card-id="${index}" 
                                 style="border-radius: 6px" 
                                 onclick="handlePlayCard(this, '${gameId}', '${dog}')">
                                
                                <img src="/assets/images/cards/${cardId}.png" width="120px" style="border-radius: 6px"/>
                            </div>
                        </div>
                    `;
                    newHandHTML += cardHtml;
                });

                if (dog) {
                    newHandHTML += `
                        <div class="mt-2">
                            <button class="btn btn-danger" onclick="handleSkipDogLife(this, '${gameId}')">Skip Dog Life</button>
                        </div>
                    `;
                }

                $handElement.html(newHandHTML);

                if (data.yourTurn) {
                    $handElement.removeClass('inactive');
                } else {
                    $handElement.addClass('inactive');
                }

                $('#current-player-name').text(data.currentPlayerName)
                if (data.nextPlayer) {
                    $('#next-player-name').text(data.nextPlayer);
                } else if (nextPlayerElement) {
                    $('#next-player-name').text("");
                } else {
                    console.warn("[DEBUG] 'current-player-name' element missing in DOM");
                }
                    $('#trump-suit').text(data.trumpSuit);
                if ($('#trick-cards-container').length) {
                    let trickHTML = '';

                    data.trickCards.forEach(trickCard => {
                        trickHTML += `
                            <div class="col-auto">
                                <div class="card text-center shadow-sm border-0 bg-transparent" style="width: 7rem; backdrop-filter: blur(4px);">
                                    <div class="p-2">
                                        <img src="/assets/images/cards/${trickCard.cardId}.png" width="100%"/>
                                    </div>
                                    <div class="card-body p-2 bg-transparent">
                                        <small class="fw-semibold text-secondary">${trickCard.player}</small>
                                    </div>
                                </div>
                            </div>
                        `;
                    });
                    $('#trick-cards-container').html(trickHTML);
                }
                if ($('#score-table-body').length && data.scoreTable) {
                    let scoreHTML = '';
                    scoreHTML += `<h4 class="fw-bold mb-3 text-black">Tricks Won</h4>

                    <div class="d-flex justify-content-between score-header pb-1">
                        <div style="width: 50%">PLAYER</div>
                        <div style="width: 50%">TRICKS</div>
                    </div>`
                    data.scoreTable.forEach(score => {
                        scoreHTML += `
                            <div class="d-flex justify-content-between score-row pt-1">
                                <div style="width: 50%" class="text-truncate">${score.name}</div>
                                <div style="width: 50%">${score.tricks}</div>
                            </div>
                        `;
                    });
                    $('#score-table-body').html(scoreHTML);
                }
                const cardId = data.firstCardId;
                if ($('#first-card-container').length) {
                    let imageSrc = '';
                    let altText = 'First Card';

                    if (cardId === "BLANK") {
                        imageSrc = "/assets/images/cards/1B.png";
                        altText = "Blank Card";
                    } else {
                        imageSrc = `/assets/images/cards/${cardId}.png`;
                    }

                    const newImageHTML = `
                        <img src="${imageSrc}" alt="${altText}" width="80px" style="border-radius: 6px"/>
                    `;

                    $('#first-card-container').html(newImageHTML);
                }
            } else if (data.status === "reloadEvent") {
                console.log("[DEBUG] Reload event received. Redirecting...");
                exchangeBody(data.content, "Knockout Whist - Ingame", data.redirectUrl);
            }
            else if (data.status === "lobbyUpdate") {
                console.log("[DEBUG] Entering 'lobbyUpdate' logic.");
                let newHtml = ''

                if (data.host) {
                    data.users.forEach(user => {

                        const inner = user.self ? `<h5 class="card-title">${user.name} (You)</h5>
                                <a href="#" class="btn btn-danger disabled" aria-disabled="true" tabindex="-1">Remove</a>`
                            : `    <h5 class="card-title">${user.name}</h5>
                                    <div class="btn btn-danger" onclick="removePlayer('${gameId}', '${user.id}')">Remove</div>`

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
                    data.users.forEach(user => {

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
                $("#players").html(newHtml);
            } else {
                console.warn(`[DEBUG] Received unknown status: ${data.status}`);
            }
        }),
        error: ((jqXHR, textStatus, errorThrown) => {
            if (jqXHR.status >= 400) {
                console.error(`Server error: ${jqXHR.status}, ${errorThrown}`);
            }
            else {
                console.error(`Something unexpected happened while polling. ${jqXHR.status}, ${errorThrown}`)
            }
        }),
        complete: (() => {
            if (!window.location.href.includes("game")) {
                console.log("[DEBUG] Page URL changed. Stopping poll restart.");
                return;
            }
            setTimeout(() => pollForUpdates(gameId), 200);
        })
    })
}

function createGameJS() {
    let lobbyName = $('#lobbyname').val();
    if ($.trim(lobbyName) === "") {
        lobbyName = "DefaultLobby"
    }
    const jsonObj = {
        lobbyname: lobbyName,
        playeramount: $("#playeramount").val()
    }
    sendGameCreationRequest(jsonObj);
}

function backToLobby(gameId) {
    const route = jsRoutes.controllers.IngameController.returnToLobby(gameId);

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    })

}

function sendGameCreationRequest(dataObject) {
    const route = jsRoutes.controllers.MainMenuController.createGame();

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        data: JSON.stringify(dataObject),
        dataType: 'json',
        success: (data => {
            if (data.status === 'success') {
                exchangeBody(data.content, "Knockout Whist - Lobby", data.redirectUrl);
            }
        }),
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    })
}

function exchangeBody(content, title = "Knockout Whist", url = null) {
    if (url) {
        window.history.pushState({}, title, url);
    }
    $("#main-body").html(content);
    document.title = title;
}

function startGame(gameId) {
    sendGameStartRequest(gameId)
}

function sendGameStartRequest(gameId) {
    const route = jsRoutes.controllers.IngameController.startGame(gameId);

    $.ajax({
        url: route.url,
        type: route.type,
        dataType: 'json',
        success: (data => {
            if (data.status === 'success') {
                window.location.href = data.redirectUrl;
            }
        }),
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    })
}
function removePlayer(gameid, playersessionId) {
    sendRemovePlayerRequest(gameid, playersessionId)
}

function sendRemovePlayerRequest(gameId, playersessionId) {
    const route = jsRoutes.controllers.IngameController.kickPlayer(gameId, playersessionId);

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        success: (data => {
            if (data.status === 'success') {
                window.location.href = data.redirectUrl;
            }
        }),
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    })
}

function login() {
    const username = $('#username').val();
    const password = $('#password').val();

    const jsonObj = {
        username: username,
        password: password
    };

    const route = jsRoutes.controllers.UserController.login_Post();
    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(jsonObj),
        success: (data => {
            if (data.status === 'success') {
                exchangeBody(data.content, 'Knockout Whist - Create Game', data.redirectUrl);
                return
            }
            alert('Login failed. Please check your credentials and try again.');
        }),
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    });
}

function joinGame() {
    const gameId = $('#gameId').val();

    const jsonObj = {
        gameId: gameId
    };

    const route = jsRoutes.controllers.MainMenuController.joinGame();
    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(jsonObj),
        success: (data => {
            if (data.status === 'success') {
                exchangeBody(data.content, "Knockout Whist - Lobby", data.redirectUrl);
                return
            }
            alert('Could not join the game. Please check the Game ID and try again.');
        }),
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    });
    return false
}

function navSpa(page, title) {
    const route = jsRoutes.controllers.MainMenuController.navSPA(page);
    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(jsonObj),
        success: (data => {
            if (data.status === 'success') {
                exchangeBody(data.content, title, data.redirectUrl);
                return
            }
            alert('Could not join the game. Please check the Game ID and try again.');
        }),
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    });
    return false
}


function selectTie(gameId) {
    const route = jsRoutes.controllers.IngameController.playTie(gameId);
    const jsonObj = {
        tie: $('#tieNumber').val()
    };

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(jsonObj),
        error: (jqXHR => {
            let error;
            try {
                error = JSON.parse(jqXHR.responseText);
            } catch (e) {
                console.error("Failed to parse error response:", e);
            }
            if (error?.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        })
    })
}

function leaveGame(gameId) {
    sendLeavePlayerRequest(gameId)
}

function sendLeavePlayerRequest(gameId) {
    const route = jsRoutes.controllers.IngameController.leaveGame(gameId);
    $.ajax({
        url: route.url,
        type: route.type,
        dataType: 'json',
        error: ((jqXHR) => {
            const errorData = JSON.parse(jqXHR.responseText);
            if (errorData && errorData.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    })
}

function handleTrumpSelection(cardobject, gameId) {
    const trumpId = cardobject.dataset.trump;
    const jsonObj = {
        trump: trumpId
    }

    const route = jsRoutes.controllers.IngameController.playTrump(gameId);

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(jsonObj),
        error: (jqXHR => {
            let error;
            try {
                error = JSON.parse(jqXHR.responseText);
            } catch (e) {
                console.error("Failed to parse error response:", e);
            }
            if (error?.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        })
    })
}

function handlePlayCard(cardobject, gameId, dog = false) {
    const cardId = cardobject.dataset.cardId;
    const jsonObj = {
        cardID: cardId
    }
    sendPlayCardRequest(jsonObj, gameId, cardobject, dog)
}

function handleSkipDogLife(cardobject, gameId) {

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

    const route = jsRoutes.controllers.IngameController.playDogCard(gameId);

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify({
            cardID: 'skip'
        }),
        error: (jqXHR => {
            let error;
            try {
                error = JSON.parse(jqXHR.responseText);
            } catch (e) {
                console.error("Failed to parse error response:", e);
            }
            if (error?.errorMessage.includes("You can't skip this round!")) {
                cardobject.parentElement.animate(wiggleKeyframes, wiggleTiming);
            } else if (error?.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        })
    })
}

function sendPlayCardRequest(jsonObj, gameId, cardobject, dog) {
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
    const route = dog === "true" ? jsRoutes.controllers.IngameController.playDogCard(gameId) : jsRoutes.controllers.IngameController.playCard(gameId);

    $.ajax({
        url: route.url,
        type: route.type,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(jsonObj),
        error: (jqXHR => {
            try {
                error = JSON.parse(jqXHR.responseText);
            } catch (e) {
                console.error("Failed to parse error response:", e);
            }
            if (error?.errorMessage.includes("You can't play this card!")) {
                cardobject.parentElement.animate(wiggleKeyframes, wiggleTiming);
            } else if (error?.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        })
    })
}

