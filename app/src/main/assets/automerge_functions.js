/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 */

let log = function (text) { document.getElementById('output').textContent += "> " + text + "\n" }

let doc = Automerge.from({ cards: [] })

let addCard = function (title, completed) {
    eventName = "automerge_addCard"
    ktchannel.startEvent(eventName)

    doc = Automerge.change(doc, 'Add card', it => {
        it.cards.push({ title: title, completed:completed })
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)
}

let removeCard = function () {
    eventName = "automerge_removeCard"
    ktchannel.startEvent(eventName)

    doc = Automerge.change(doc, 'Delete card', it => {
      delete it.cards[0]
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)
}

let setCardCompleted = function (index, completed) {
    eventName = "automerge_setCardCompleted"
    ktchannel.startEvent(eventName)

    doc = Automerge.change(doc, 'Set Completed', it => {
      it.cards[index].completed = completed
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)
}

let getDocumentState = function() {
    eventName = "automerge_getDocumentState"
    ktchannel.startEvent(eventName)

    docState = Automerge.save(doc)
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
    log("* " + docState)

    ktchannel.endEvent(eventName)
    return docState
}

let setDocumentState = function(docState) {
    eventName = "automerge_setDocumentState"
    ktchannel.startEvent(eventName)

    doc = Automerge.load(docState)
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)
}

let clearState = function() {
    eventName = "automerge_clearState"
    ktchannel.startEvent(eventName)

    doc = Automerge.from({ cards: [] })
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(eventName)
}