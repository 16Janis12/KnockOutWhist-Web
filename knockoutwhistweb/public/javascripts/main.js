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
    sendPlayCardRequest(jsonObj, gameId)
}

function sendPlayCardRequest(jsonObj, gameId) {
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

