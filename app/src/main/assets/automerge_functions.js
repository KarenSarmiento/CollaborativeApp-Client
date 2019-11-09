/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 */

// TODO: Change to camel case

let log = function (text) { document.getElementById('output').textContent += "> " + text + "\n" }

let doc = Automerge.from({ cards: [] })

let addCard = function (title, completed) {
    event_name = "automerge_addCard"
    ktchannel.startEvent(event_name)

    doc = Automerge.change(doc, 'Add card', it => {
        it.cards.push({ title: title, completed:completed })
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(event_name)
}

let removeCard = function () {
    event_name = "automerge_removeCard"
    ktchannel.startEvent(event_name)

    doc = Automerge.change(doc, 'Delete card', it => {
      delete it.cards[0]
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(event_name)
}

let setCardCompleted = function (index, completed) {
    event_name = "automerge_setCardCompleted"
    ktchannel.startEvent(event_name)

    doc = Automerge.change(doc, 'Set Completed', it => {
      it.cards[index].completed = completed
    })
    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(event_name)
}

let getDocumentState = function() {
    doc_state = Automerge.save(doc)
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
    log("* " + doc_state)
    return doc_state
}

let setDocumentState = function(docState) {
    doc = Automerge.load(docState)
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
}

let clearState = function() {
    doc = Automerge.from({ cards: [] })
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
}