var canPlayCard = false;
const PlayerHandComponent = {
    data() {
        return {
            hand: [],
            isDogPhase: false,
            isAwaitingResponse: false,
        };
    },
    computed: {
        isHandInactive() {
            //TODO: Needs implementation
        }
    },
    template: `
        <div class="row justify-content-center g-2 mt-4 bottom-div" 
             style="backdrop-filter: blur(4px); margin-left: 0; margin-right: 0;">
            
            <div id="card-slide" class="row justify-content-center ingame-cards-slide" :class="{'inactive': isHandInactive }">
                
                <div v-for="card in hand" :key="card.idx" class="col-auto handcard" style="border-radius: 6px">
                    <div class="btn btn-outline-light p-0 border-0 shadow-none" 
                         :data-card-id="card.idx" 
                         style="border-radius: 6px" 
                         @click="handlePlayCard(card.idx)">
                        
                        <img :src="getCardImagePath(card.card)" width="120px" style="border-radius: 6px" :alt="card.card"/>
                    </div>
                </div>

                <div v-if="isDogPhase" class="mt-2">
                    <button class="btn btn-danger" @click="handleSkipDogLife()">Skip Turn</button>
                </div>
            </div>
        </div>
    `,
    methods: {
        updateHand(eventData) {
            this.hand = eventData.hand.map(card => ({
                idx: parseInt(card.idx, 10),
                card: card.card
            }));
            this.isDogPhase = false;

            console.log("Vue Data Updated. Hand size:", this.hand.length);

            if (this.hand.length > 0) {
                console.log("First card path check:", this.getCardImagePath(this.hand[0].card));
            }
        },
        handlePlayCard(cardidx) {
            if(this.isAwaitingResponse) return
            if(!canPlayCard) return
            canPlayCard = false;
            this.isAwaitingResponse = true


            console.debug(`Playing card ${cardidx} from hand`)

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
            const targetButton = this.$el.querySelector(`[data-card-id="${cardidx}"]`);
            const cardElement = targetButton ? targetButton.closest('.handcard') : null;

            const payload = {
                cardindex: cardidx.toString(),
                isDog: false
            }
            sendEventAndWait("PlayCard", payload).then(
                () => {
                    this.hand = this.hand.filter(card => card.idx !== cardidx);

                    this.hand.forEach((card, index) => {
                        card.idx = index;
                    })
                    this.isAwaitingResponse = false;
                }
            ).catch(
                (err) => {
                    if (cardElement) {
                        cardElement.animate(wiggleKeyframes, wiggleTiming);
                    } else {
                        console.warn(`Could not find DOM element for card index ${cardidx} to wiggle.`);
                    }
                    this.isAwaitingResponse = false;
                    canPlayCard = true;

                }
            )
        },
        handleSkipDogLife() {
            globalThis.handleSkipDogLife();
        },
        getCardImagePath(cardName) {
            return `/assets/images/cards/${cardName}.png`;
        }
    }
};
const ScoreBoardComponent = {
    data() {
        return {
            trumpsuit: 'N/A',
            playerScores: [],
        };
    },
    template: `
        <div class="score-table mt-5" id="score-table-container">
            <h4 class="fw-bold mb-3 text-black">Tricks Won</h4>

            <div class="d-flex justify-content-between score-header pb-1">
                <div style="width: 50%">PLAYER</div>
                <div style="width: 50%">TRICKS</div>
            </div>

            <div id="score-table-body">
                <div v-for="(player, index) in playerScores" 
                     :key="player.name" 
                     class="d-flex justify-content-between score-row pt-1">
                    
                    <div style="width: 50%" class="text-truncate">
                        {{ player.name }}
                    </div>
                    
                    <div style="width: 50%">
                        {{ player.tricks }}
                    </div>
                </div>
            </div>
        </div>
    `,

    methods: {
        calculateNewScores(players, tricklist) {
            const playercounts = new Map();
            players.forEach(player => {
                playercounts.set(player, 0)
            });

            tricklist.forEach(playerWonTrick => {
                if (playerWonTrick !== "Trick in Progress" && playercounts.has(playerWonTrick)) {
                    playercounts.set(playerWonTrick, playercounts.get(playerWonTrick) + 1);
                }
            });

            const newScores = players.map(name => ({
                name: name,
                tricks: playercounts.get(name) || 0,
            }));

            newScores.sort((a, b) => b.tricks - a.tricks);

            return newScores;
        },

        updateNewRoundData(eventData) {
            console.log("Vue Scoreboard Data Update Triggered: New Round!");

            this.playerScores = eventData.players.map(player => ({
                name: player,
                tricks: 0,
            }));
        },

        updateTrickEndData(eventData) {
            const { playerwon, playersin, tricklist } = eventData;

            console.log(`Vue Scoreboard Data Update Triggered: ${playerwon} won the trick!`);

            this.playerScores = this.calculateNewScores(playersin, tricklist);


        }
    }
};
const GameInfoComponent = {
    data() {
        return {
            trumpsuit: 'No Trumpsuit',
            firstCardImagePath: '/assets/images/cards/1B.png',
        };
    },

    template: `
        <div>
            <h4 class="fw-semibold mb-1">Trumpsuit</h4>
            <p class="fs-5 text-primary" id="trumpsuit">{{ trumpsuit }}</p>


            <h5 class="fw-semibold mt-4 mb-1">First Card</h5>
            <div class="d-inline-block border rounded shadow-sm p-1 bg-light" id="first-card-container">

                <img :src="firstCardImagePath" alt="First Card" width="80px" style="border-radius: 6px"/>
            </div>
        </div>
    `,

    methods: {
        resetFirstCard(eventData) {
            console.log("GameInfoComponent: Resetting First Card to placeholder.");
            this.firstCardImagePath = '/assets/images/cards/1B.png';
        },
        updateFirstCard(eventData) {
            const firstCardId = eventData.firstCard;
            console.log("GameInfoComponent: Updating First Card to:", firstCardId);

            let imageSource;
            if (firstCardId === "BLANK" || !firstCardId) {
                imageSource = "/assets/images/cards/1B.png";
            } else {
                imageSource = `/assets/images/cards/${firstCardId}.png`;
            }
            this.firstCardImagePath = imageSource;
        },
        updateTrumpsuit(eventData) {
            this.trumpsuit = eventData.trumpsuit;
        }
    }
};
const TrickDisplayComponent = {
    data() {
        return {
            playedCards: [],
        };
    },

    template: `
        <div class="d-flex justify-content-center g-3" id="trick-cards-content">
            <div v-for="(play, index) in playedCards" :key="index" class="col-auto">
                <div class="card text-center shadow-sm border-0 bg-transparent" style="width: 7rem;
                    backdrop-filter: blur(4px);">
                    <div class="p-2">
                        <img :src="getCardImagePath(play.cardId)" width="100%" style="border-radius: 6px"/>
                    </div>
                    <div class="card-body p-2 bg-transparent">
                        <small class="fw-semibold text-secondary">{{ play.player }}</small>
                    </div>
                </div>
            </div>
        </div>
    `,

    methods: {
        getCardImagePath(cardId) {
            return `/assets/images/cards/${cardId}.png`;
        },

        clearPlayedCards() {
            console.log("TrickDisplayComponent: Clearing played cards.");
            this.playedCards = [];
        },

        updatePlayedCards(eventData) {
            console.log("TrickDisplayComponent: Updating played cards.");

            this.playedCards = eventData.playedCards;
        }
    }
};
function formatPlayerName(player) {
    let name = player.name;
    if (player.dog) {
        name += " 🐶";
    }
    return name;
}

const TurnComponent = {
    data() {
        return {
            currentPlayerName: 'Waiting...',
            nextPlayers: [],
        };
    },

    template: `
        <div class="turn-tracker-container">
            <h4 class="fw-semibold mb-1">Current Player</h4>
            <p class="fs-3 fw-bold text-success" id="current-player-name">{{ currentPlayerName }}</p>

            <div v-if="nextPlayers.length > 0">
                <h5 class="fw-semibold mt-4 mb-1" id="next-players-text">Next Players</h5>
                <div id="next-players-container">
                    <p v-for="name in nextPlayers" :key="name" class="fs-5 text-primary">{{ name }}</p>
                </div>
            </div>
        </div>
    `,
    methods: {
        updateTurnData(eventData) {
            console.log("TurnComponent: Updating turn data.");
            const { currentPlayer, nextPlayers } = eventData;

            this.currentPlayerName = formatPlayerName(currentPlayer);

            this.nextPlayers = nextPlayers.map(player => formatPlayerName(player));
        }
    }
};
const LobbyComponent = {
    data() {
        return {
            lobbyName: 'Loading...',
            lobbyId: 'default',
            isHost: false,
            maxPlayers: 0,
            players: [],
            showKickedModal: false,
            kickedEventData: null,
            showSessionClosedModal: false,
            sessionClosedEventData: null,
        };
    },

    template: `
        <main class="lobby-background vh-100" id="lobbybackground">
            
            <div v-if="showKickedModal" class="modal fade show d-block" 
                 tabindex="-1" 
                 role="dialog" 
                 aria-labelledby="kickedModalTitle" 
                 aria-modal="true" 
                 style="background-color: rgba(0,0,0,0.5);">
                
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="kickedModalTitle">Kicked</h5>
                        </div>
                        <div class="modal-body">
                            <p>You've been kicked from the lobby.</p>
                            <p class="text-muted small">You'll get redirected to the mainmenu in 5 seconds...</p>
                        </div>
                    </div>
                </div>
            </div>


            <div v-if="showSessionClosedModal" class="modal fade show d-block" 
                 tabindex="-1" 
                 role="dialog" 
                 aria-labelledby="sessionClosedModalTitle" 
                 aria-modal="true" 
                 style="background-color: rgba(0,0,0,0.5);">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="sessionClosedModalTitle">Session Closed</h5>
                        </div>
                        <div class="modal-body">
                            <p>The session was closed.</p>
                            <p class="text-muted small">You'll get redirected to the mainmenu in 5 seconds...</p>
                        </div>
                    </div>
                </div>
            </div>

            <div class="container d-flex flex-column" style="height: calc(100vh - 1rem);">
                
                <div class="row">
                    <div class="col">
                        <div class="p-3 fs-1 d-flex align-items-center">
                            <div class="text-center" style="flex-grow: 1;">
                                Lobby-Name: {{ lobbyName }}
                            </div>
                            <div class="btn btn-danger ms-auto" @click="leaveGame(lobbyId)">Exit</div>
                        </div>
                    </div>
                </div>
                
                <div class="row">
                    <div class="col">
                        <div class="p-3 text-center fs-4" id="playerAmount">
                            Players: {{ players.length }} / {{ maxPlayers }}
                        </div>
                    </div>
                </div>
                
                <div class="row justify-content-center align-items-center flex-grow-1">
                    
                    <template v-if="isHost">
                        <div id="players" class="justify-content-center align-items-center d-flex flex-wrap">
                            <div v-for="player in players" :key="player.id" class="col-auto my-auto m-3">
                                <div class="card" style="width: 18rem;">
                                    <img src="/assets/images/profile.png" alt="Profile" class="card-img-top w-50 mx-auto mt-3" />
                                    <div class="card-body">
                                        <h5 class="card-title">
                                            {{ player.name }} <span v-if="player.self">(You)</span>
                                        </h5>
                                        
                                        <template v-if="player.self">
                                            <a href="#" class="btn btn-danger disabled" aria-disabled="true" tabindex="-1">Remove</a>
                                        </template>
                                        <template v-else>
                                            <div class="btn btn-danger" @click="handleKickPlayer(player.id)">Remove</div>
                                        </template>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-12 text-center mb-5">
                            <div class="btn btn-success" @click="startGame()">Start Game</div>
                        </div>
                    </template>
                    
                    <template v-else>
                        <div id="players" class="justify-content-center align-items-center d-flex flex-wrap">
                            <div v-for="player in players" :key="player.id" class="col-auto my-auto m-3"> 
                                <div class="card" style="width: 18rem;">
                                    <img src="/assets/images/profile.png" alt="Profile" class="card-img-top w-50 mx-auto mt-3" />
                                    <div class="card-body">
                                        <h5 class="card-title">
                                            {{ player.name }} <span v-if="player.self">(You)</span>
                                        </h5>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-12 text-center mt-3">
                            <p class="fs-4">Waiting for the host to start the game...</p>
                            <div class="spinner-border mt-1" role="status">
                                <span class="visually-hidden">Loading...</span>
                            </div>
                        </div>
                    </template>
                    
                </div>
            </div>
        </main>
    `,

    methods: {
        updateLobbyData(eventData) {
            console.log("LobbyComponent: Received Lobby Update Event.");

            this.isHost = eventData.host;
            this.maxPlayers = eventData.maxPlayers;
            this.players = eventData.players;
        },

        setInitialData(name, id) {
            this.lobbyName = name;
            this.lobbyId = id;
        },
        startGame() {
            globalThis.startGame()
        },
        leaveGame(gameId) {
            //TODO: Needs implementation
        },
        handleKickPlayer(playerId) {
            globalThis.handleKickPlayer(playerId)
        },
        showKickModal(eventData) {
            this.showKickedModal = true;
            setTimeout(() => {
                this.kickedEventData = eventData;
                this.showKickedModal = false;

                if (typeof globalThis.receiveGameStateChange === 'function') {
                    globalThis.receiveGameStateChange(this.kickedEventData);
                } else {
                    console.error("FATAL: receiveGameStateChange ist nicht global definiert.");
                }
            }, 5000);
        },
        showSessionClosedModal(eventData) {
            this.sessionClosedEventData = eventData;
            this.showSessionClosedModal = true;

            setTimeout(() => {
                this.showSessionClosedModal = false;

                if (typeof globalThis.receiveGameStateChange === 'function') {
                    globalThis.receiveGameStateChange(this.sessionClosedEventData);
                } else {
                    console.error("FATAL: receiveGameStateChange ist nicht global definiert.");
                }
            }, 5000);
        }
    }
};

function requestCardEvent(eventData) {
    //TODO: Needs correct implementation of setting the inactive class in the PlayerHandComponent
}
function receiveGameStateChange(eventData) {
    const content = eventData.content;
    const title = eventData.title || 'Knockout Whist';
    const url = eventData.url || null;

    exchangeBody(content, title, url);
}
function receiveRoundEndEvent(eventData) {
    //TODO: When alert is working, set an alert that shows how won the round and with how much tricks.
}
let playerHandApp = null;
let scoreBoardApp = null;
let gameInfoApp = null;
let trickDisplayApp = null;
let turnApp = null;
globalThis.initGameVueComponents = function() {
        // Initializing PlayerHandComponent
        const app = Vue.createApp(PlayerHandComponent);

        playerHandApp = app;
        const mountedHand = app.mount('#player-hand-container');

        if (mountedHand && mountedHand.updateHand) {
            globalThis.updatePlayerHand = mountedHand.updateHand;
            onEvent("ReceivedHandEvent", globalThis.updatePlayerHand);
            console.log("PLAYER HAND SYSTEM: updatePlayerHand successfully exposed.");
        } else {
            console.error("FATAL ERROR: PlayerHandComponent mount failed. Check if #player-hand-container exists.");
        }

        // Initializing Scoreboard
        if (scoreBoardApp) return

        const app2 = Vue.createApp(ScoreBoardComponent)
        scoreBoardApp = app2
        const mountedHand2 = app2.mount('#score-table')
        if (mountedHand2) {
            globalThis.updateNewRoundData = mountedHand2.updateNewRoundData;
            onEvent("NewRoundEvent", handleNewRoundEvent);

            globalThis.updateTrickEndData = mountedHand2.updateTrickEndData;
            onEvent("TrickEndEvent", globalThis.updateTrickEndData);
            console.log("SCOREBOARD: updateNewRoundData successfully exposed.");
        } else {
            console.error("FATAL ERROR: Scoreboard mount failed. Check if #score-table exists.");
        }
        // Initializing Gameinfo
        if (gameInfoApp) return

        const app3 = Vue.createApp(GameInfoComponent)
        gameInfoApp = app3
        const mountedGameInfo = app3.mount('#game-info-component')
        if(mountedGameInfo) {
            globalThis.resetFirstCard = mountedGameInfo.resetFirstCard;
            globalThis.updateFirstCard = mountedGameInfo.updateFirstCard;
            globalThis.updateTrumpsuit = mountedGameInfo.updateTrumpsuit
            onEvent("NewTrickEvent", handleNewTrickEvent);
            console.log("GameInfo: resetFirstCard successfully exposed.");
        } else {
            console.error("FATAL ERROR: GameInfo mount failed. Check if #score-table exists.");
        }

        // Initializing TrickCardContainer
        if (trickDisplayApp) return;
        const app4 = Vue.createApp(TrickDisplayComponent);
        trickDisplayApp = app4;
        const mountedTrickDisplay = app4.mount('#trick-cards-container');

        if (mountedTrickDisplay) {
            globalThis.clearPlayedCards = mountedTrickDisplay.clearPlayedCards;
            globalThis.updatePlayedCards = mountedTrickDisplay.updatePlayedCards;
            onEvent("CardPlayedEvent", handleCardPlayedEvent)
            console.log("TRICK DISPLAY: Handlers successfully exposed (clearPlayedCards, updatePlayedCards).");
        } else {
            console.error("FATAL ERROR: TrickDisplay mount failed. Check if #trick-cards-container exists.");
        }

        // Initializing TurnContainer
        if (turnApp) return;
        const app5 = Vue.createApp(TurnComponent)
        turnApp = app5;
        const mountedTurnApp = app5.mount('#turn-component')

        if(mountedTurnApp) {
            globalThis.updateTurnData = mountedTurnApp.updateTurnData;
            onEvent("TurnEvent", globalThis.updateTurnData);
            console.log("TURN DISPLAY: Handlers successfully exposed (clearPlayedCards, updatePlayedCards).");
        } else {
            console.error("FATAL ERROR: TURNAPP mount failed. Check if #trick-cards-container exists.");
        }
}
let lobbyApp = null;
globalThis.initLobbyVueComponents = function(initialLobbyName, initialLobbyId, initialIsHost, initialMaxPlayers, initialPlayers) {

    if (lobbyApp) return;

    const appLobby = Vue.createApp(LobbyComponent);
    lobbyApp = appLobby;
    const mountedLobby = appLobby.mount('#lobby-app-mount');

    if (mountedLobby) {
        mountedLobby.setInitialData(initialLobbyName, initialLobbyId);

        //Damit beim erstmaligen Betreten der Lobby die Spieler etc. angezeigt werden.
        mountedLobby.updateLobbyData({
            host: initialIsHost,
            maxPlayers: initialMaxPlayers,
            players: initialPlayers
        });

        globalThis.updateLobbyData = mountedLobby.updateLobbyData;
        globalThis.showKickModal = mountedLobby.showKickModal;
        globalThis.showSessionClosedModal = mountedLobby.showSessionClosedModal;
        onEvent("LobbyUpdateEvent", globalThis.updateLobbyData);
        onEvent("KickEvent", globalThis.showKickModal);
        onEvent("SessionClosed", globalThis.showSessionClosedModal);
        console.log("LobbyComponent successfully mounted and registered events.");
    } else {
        console.error("FATAL ERROR: LobbyComponent mount failed.");
    }
}
function handleCardPlayedEvent(eventData) {
    console.log("CardPlayedEvent received. Updating Game Info and Trick Display.");

    if (typeof globalThis.updateFirstCard === 'function') {
        globalThis.updateFirstCard(eventData);
    }

    if (typeof globalThis.updatePlayedCards === 'function') {
        globalThis.updatePlayedCards(eventData);
    }
}
function handleNewTrickEvent(eventData) {
    if (typeof globalThis.resetFirstCard === 'function') {
        globalThis.resetFirstCard(eventData);
    }

    if (typeof globalThis.clearPlayedCards === 'function') {
        globalThis.clearPlayedCards();
    }
}
function handleNewRoundEvent(eventData) {
    if (typeof globalThis.updateNewRoundData === 'function') {
        globalThis.updateNewRoundData(eventData);
    }
    if (typeof globalThis.updateTrumpsuit === 'function') {
        globalThis.updateTrumpsuit(eventData);
    }
}

onEvent("GameStateChangeEvent", receiveGameStateChange)
onEvent("LeftEvent", receiveGameStateChange)
onEvent("RequestCardEvent", requestCardEvent)
onEvent("RoundEndEvent", receiveRoundEndEvent)