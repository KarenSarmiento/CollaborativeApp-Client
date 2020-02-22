/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 */

// TODO: COPY BEFORE MAKING CHANGES FOR ATOMICITY.

let log = function (text) { document.getElementById('output').textContent += "> " + text + "\n" }

let doc = Automerge.from({ cards: [] })

let applyJsonUpdate = function(changes) {
    doc = Automerge.applyChanges(doc, changes)
    log("> " + JSON.stringify(doc.cards))
}

let addCard = function (title, completed) {
    let eventName = "automerge_addCard"
    ktchannel.startEvent(eventName)

    let newDoc = Automerge.change(doc, eventName, it => {
        it.cards.push({ title: title, completed:completed })
    })
    let changes = Automerge.getChanges(doc, newDoc)
    doc = newDoc

    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
    ktchannel.endEvent(eventName)
    return changes
}

let removeCard = function (index) {
    let eventName = "automerge_removeCard"
    ktchannel.startEvent(eventName)

    let newDoc = Automerge.change(doc, eventName, it => {
      delete it.cards[index]
    })
    let changes = Automerge.getChanges(doc, newDoc)
    doc = newDoc

    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
    ktchannel.endEvent(eventName)
    return changes
}

let setCardCompleted = function (index, completed) {
    eventName = "automerge_setCardCompleted"
    ktchannel.startEvent(eventName)

    let newDoc = Automerge.change(doc, eventName, it => {
      it.cards[index].completed = completed
    })
    let changes = Automerge.getChanges(doc, newDoc)
    doc = newDoc

    log("> " + JSON.stringify(doc.cards))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
    ktchannel.endEvent(eventName)
    return changes
}
