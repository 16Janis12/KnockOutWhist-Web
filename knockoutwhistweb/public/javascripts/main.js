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
            if (errorData?.errorMessage) {
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
            if (errorData?.errorMessage) {
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
            if (errorData?.errorMessage) {
                alert(`${errorData.errorMessage}`);
            } else {
                alert(`An unexpected error occurred. Please try again. Status: ${jqXHR.status}`);
            }
        })
    });
    return false
}