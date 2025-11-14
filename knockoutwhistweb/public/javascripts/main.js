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
    if (!gameId) {
        console.error("Game ID is missing. Stopping poll.");
        return;
    }
    const element = document.getElementById('card-slide');
    const element2 = document.getElementById('lobbybackground');
    // Safety check for the target element
    if (!element && !element2) {
        console.error("Polling target element not found. Stopping poll.");
        // Use a timeout to retry in case the DOM loads late, passing gameId.
        setTimeout(() => pollForUpdates(gameId), 5000);
        return;
    }
    const route = jsRoutes.controllers.PollingController.polling(gameId);

    // Call your specific controller endpoint
    fetch(route.url)
        .then(response => {
            if (response.status === 204) {
                console.log("Polling: Timeout reached. Restarting poll.");

                // CRITICAL: Pass gameId in the recursive call
                setTimeout(() => pollForUpdates(gameId), 5000);
            } else if (response.ok && response.status === 200) {
                response.json().then(data => {

                    if (data.status === "cardPlayed" && data.handData && element) {
                        console.log("Event received: Card played. Redrawing hand.");

                        const newHand = data.handData;
                        let newHandHTML = '';
                        element.innerHTML = '';

                        if(data.animation) {
                            if (!element.classList.contains('ingame-cards-slide')) {
                                element.classList.add('ingame-cards-slide');
                            }
                        } else {
                            element.classList.remove('ingame-cards-slide');
                        }

                        newHand.forEach((cardId, index) => {
                            const cardHtml = `
                                <div class="col-auto handcard" style="border-radius: 6px">
                                    <div class="btn btn-outline-light p-0 border-0 shadow-none" 
                                         data-card-id="${index}" 
                                         style="border-radius: 6px" 
                                         onclick="handlePlayCard(this, '${gameId}')">
                                        
                                        <img src="/assets/images/cards/${cardId}.png" width="120px" style="border-radius: 6px"/>
                                    </div>
                                </div>
                            `;
                            newHandHTML += cardHtml;
                        });

                        element.innerHTML = newHandHTML;

                        const currentPlayerElement = document.getElementById('current-player-name');
                        if (currentPlayerElement) {
                            currentPlayerElement.textContent = data.currentPlayerName;
                        }
                        const nextPlayerElement = document.getElementById('next-player-name');
                        if (nextPlayerElement && data.nextPlayer) {
                            // Use the correctly named field from the server response
                            nextPlayerElement.textContent = data.nextPlayer;
                        } else {
                            // Case 2: Player name is empty or null (signal to clear display).
                            nextPlayerElement.textContent = "";
                        }

                        const trumpElement = document.getElementById('trump-suit');
                        if (trumpElement) {
                            trumpElement.textContent = data.trumpSuit;
                        }
                        const trickContainer = document.getElementById('trick-cards-container');
                        if (trickContainer) {
                            let trickHTML = '';

                            // Iterate over the array of played cards received from the server
                            data.trickCards.forEach(trickCard => {
                                // Reconstruct the HTML structure from your template
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
                            trickContainer.innerHTML = trickHTML;
                        }
                        const scoreBody = document.getElementById('score-table-body');
                        if (scoreBody && data.scoreTable) {
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
                            scoreBody.innerHTML = scoreHTML;
                        }
                        const firstCardContainer = document.getElementById('first-card-container');
                        const cardId = data.firstCardId; // This will be "KH", "S7", or "BLANK"

                        if (firstCardContainer) {
                            let imageSrc = '';
                            let altText = 'First Card';

                            // Check if a card was actually played or if it's the start of a trick
                            if (cardId === "BLANK") {
                                imageSrc = "/assets/images/cards/1B.png";
                                altText = "Blank Card";
                            } else {
                                imageSrc = `/assets/images/cards/${cardId}.png`;
                            }

                            // Reconstruct the image HTML (assuming the inner element needs replacement)
                            const newImageHTML = `
                                <img src="${imageSrc}" alt="${altText}" width="80px" style="border-radius: 6px"/>
                            `;

                            // Clear the container and insert the new image
                            firstCardContainer.innerHTML = newImageHTML;
                        }
                    } else if (data.status === "reloadEvent") {
                        window.location.href = data.redirectUrl;
                    } else if (data.status === "lobbyUpdate") {
                        const players = document.getElementById("players");
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
                        players.innerHTML = newHtml;
                    }
                    pollForUpdates(gameId);
                });
            } else {
                // Handle network or server errors
                console.error(`Polling error: Status ${response.status}`);
                // Wait before retrying, passing gameId correctly
                setTimeout(() => pollForUpdates(gameId), 5000);
            }
        })
        .catch(error => {
            console.error("Network error during polling:", error);
            // Wait before retrying on network failure, passing gameId correctly
            setTimeout(() => pollForUpdates(gameId), 5000);
        });
}

function createGameJS() {
    let lobbyName = document.getElementById("lobbyname").value;
    if (lobbyName === "") {
        lobbyName = "DefaultLobby"
    }
    const playerAmount = document.getElementById("playeramount").value;
    const jsonObj = {
        lobbyname: lobbyName,
        playeramount: playerAmount
    }
    sendGameCreationRequest(jsonObj);
}

function sendGameCreationRequest(dataObject) {
    const route = jsRoutes.controllers.MainMenuController.createGame();

    fetch(route.url, {
        method: route.type,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(dataObject)
    })
        .then(response => {
            return response.json().then(data => {
                if (!response.ok) {
                    return Promise.reject(data);
                }
                return data;
            });
        })
        .then(data => {
            if (data.status === 'success') {
                window.location.href = data.redirectUrl;
            }
        })
        .catch(error => {
            if (error && error.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        });
}
function startGame(gameId) {
    sendGameStartRequest(gameId)
}
function sendGameStartRequest(gameId) {
    const route = jsRoutes.controllers.IngameController.startGame(gameId);

    fetch(route.url, {
        method: route.type,
    })
        .then(response => {
            return response.json().then(data => {
                if (!response.ok) {
                    return Promise.reject(data);
                }
                return data;
            });
        })
        .then(data => {
            // SUCCESS BLOCK: data is the { status: 'success', ... } object
            if (data.status === 'success') {
                window.location.href = data.redirectUrl;
            }
        })
        .catch(error => {
            if (error && error.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        });
}
function removePlayer(gameid, playersessionId) {
    sendRemovePlayerRequest(gameid, playersessionId)
}

function sendRemovePlayerRequest(gameId, playersessionId) {
    const route = jsRoutes.controllers.IngameController.kickPlayer(gameId, playersessionId);

    fetch(route.url, {
        method: route.type,
        headers: {
            'Content-Type': 'application/json',
        }
    })
        .then(response => {
            return response.json().then(data => {
                if (!response.ok) {
                    return Promise.reject(data);
                }
                return data;
            });
        })
        .then(data => {
            // SUCCESS BLOCK: data is the { status: 'success', ... } object
            if (data.status === 'success') {
                window.location.href = data.redirectUrl;
            }
        })
        .catch(error => {
            if (error && error.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        });
}

function leaveGame(gameId) {
    sendLeavePlayerRequest(gameId)
}

function sendLeavePlayerRequest(gameId) {

    const route = jsRoutes.controllers.IngameController.leaveGame(gameId);
    fetch(route.url, {
        method: route.type,
    })
        .then(response => {
            return response.json().then(data => {
                if (!response.ok) {
                    return Promise.reject(data);
                }
                return data;
            });
        })
        .then(data => {
            // SUCCESS BLOCK: data is the { status: 'success', ... } object
            if (data.status === 'success') {
                window.location.href = data.redirectUrl;
            }
        })
        .catch(error => {
            if (error && error.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        });
}

function handlePlayCard(cardobject, gameId) {
    const cardId = cardobject.dataset.cardId;
    const jsonObj = {
        cardID: cardId
    }
    sendPlayCardRequest(jsonObj, gameId, cardobject)
}

function sendPlayCardRequest(jsonObj, gameId, cardobject) {
    const wiggleKeyframes = [
        { transform: 'translateX(0)' },
        { transform: 'translateX(-5px)' },
        { transform: 'translateX(5px)' },
        { transform: 'translateX(-5px)' },
        { transform: 'translateX(0)' }
    ];

    // Define the timing options
    const wiggleTiming = {
        duration: 400, // 0.4 seconds
        iterations: 1,
        easing: 'ease-in-out',
        // Fill mode ensures the final state is applied until reset
        fill: 'forwards'
    };
    const route = jsRoutes.controllers.IngameController.playCard(gameId);

    fetch(route.url, {
        method: route.type,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(jsonObj)
    })
        .then(response => {
            return response.json().then(data => {
                if (!response.ok) {
                    return Promise.reject(data);
                }
                return data;
            });
        })
        .then(data => {
            if (data.status === 'success') {
                //window.location.href = data.redirectUrl;
            }
        })
        .catch(error => {
            if (error && error.errorMessage.includes("You can't play this card!")) {
                cardobject.parentElement.animate(wiggleKeyframes, wiggleTiming);
            } else if (error && error.errorMessage) {
                alert(`${error.errorMessage}`);
            } else {
                alert('An unexpected error occurred. Please try again.');
            }
        });
}

