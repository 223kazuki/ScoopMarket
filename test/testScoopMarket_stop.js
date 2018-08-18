"use strict";

var ScoopMarket = artifacts.require("ScoopMarket");

const checkReverted = async (promise) => {
    try {
        await promise;
    } catch (err) {
        assert(err.message.search("revert") >= 0)
        return;
    }
    assert.fail(0, 1, 'Expected throw not received');
}

/**
 * These tests covers the behaviour of emergency stop.
 * I tested it because we can't stop atacker to execute critical functions
 * if there is a bug in this functionality.
 */
contract('ScoopMarket', ([_1, _2, _3, _4, _5, _6, owner, seller, buyer]) => {
    let scoopMarket;
    const testHash = "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr";

    beforeEach('setup contract for each test', async function () {
        scoopMarket = await ScoopMarket.new({ from: owner });
    })

    // System requirements
    it("should stop mint emergencily.", async () => {
        // Paused.
        await scoopMarket.pause({ from: owner });
        checkReverted(
            scoopMarket.mint("scoop0", 1*10**14, true,  testHash, { from: seller })
        );
        // Unpaused.
        await scoopMarket.unpause({ from: owner });
        await scoopMarket.mint("scoop0", 1*10**14, true,  testHash, { from: seller });
    });

    it("should stop purchase emergencily.", async () => {
        await scoopMarket.mint("scoop0", 1*10**14, true,  testHash, { from: seller });
        await scoopMarket.request(0, { from: buyer });
        await scoopMarket.approve(buyer, 0, { from: seller });
        // Paused.
        await scoopMarket.pause({ from: owner });
        checkReverted(
            scoopMarket.purchase(0, { from: buyer, value: 1*10**14 })
        );
        // Unpaused.
        await scoopMarket.unpause({ from: owner });
        await scoopMarket.purchase(0, { from: buyer, value: 1*10**14 });
    });

    it("should stop withdrawal emergencily.", async () => {
        await scoopMarket.mint("scoop0", 1*10**14, true,  testHash, { from: seller });
        await scoopMarket.request(0, { from: buyer });
        await scoopMarket.approve(buyer, 0, { from: seller });
        await scoopMarket.purchase(0, { from: buyer, value: 1*10**14 });
        // Paused.
        await scoopMarket.pause({ from: owner });
        checkReverted(
            scoopMarket.withdrawPayments({ from: seller })
        );
        // Unpaused.
        await scoopMarket.unpause({ from: owner });
        await scoopMarket.withdrawPayments({ from: seller });
    });

});
