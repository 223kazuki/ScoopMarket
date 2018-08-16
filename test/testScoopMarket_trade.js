"use strict";

var ScoopMarket = artifacts.require("ScoopMarket");

const checkReverted = async (promise) => {
    try {
        await promise;
    } catch (err) {
        assert(err.message.search("revert") >= 0, "Reverted.")
        return;
    }
    assert.fail(0, 1, 'Expected, but not reverted.');
}

contract('ScoopMarket', ([_1, _2, owner, seller, buyer, other]) => {
    let scoopMarket;
    const testHash = "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr";

    before('setup contract for all test', async function () {
        scoopMarket = await ScoopMarket.new({ from: owner });
        await scoopMarket.mint("scoop0", 1*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop1", 2*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop2", 3*10**14, false, testHash, { from: seller });
        await scoopMarket.mint("scoop3", 4*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop4", 5*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop5", 6*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop6", 7*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop7", 8*10**14, true,  testHash, { from: seller });
        await scoopMarket.mint("scoop8", 9*10**14, true,  testHash, { from: seller });
    })

    it("buyer should be able to purchase only approved token.", async () => {
        // Request 2 tokens.
        await scoopMarket.request(0, { from: buyer });
        await scoopMarket.request(1, { from: buyer });
        // Approve only one.
        await scoopMarket.approve(buyer, 0, { from: seller });

        // Other can't purchase approved token.
        checkReverted(
            scoopMarket.purchase(0, { from: other, value: 1*10**14 })
        );
        
        // Purchase success only approved one.
        await scoopMarket.purchase(0, { from: buyer, value: 1*10**14 });
        checkReverted(
            scoopMarket.purchase(1, { from: buyer, value: 2*10**14 })
        );

        // Buyer get token.
        let balance = await scoopMarket.balanceOf(buyer);
        assert(balance.toNumber() == 1, "Purchased seuccesfully.");
        let tokens = await scoopMarket.scoopsOf(buyer);
        assert(tokens[0].toNumber() === 0, "Token 0 owned by buyer.");

        // Token forSale flag reset false. 
        let tokensForSale = await scoopMarket.scoopsForSale();
        assert(!tokensForSale[0], "Token 0 is not for sale.");

        // Seller can withdraw credit.
        let balanceBefore = web3.eth.getBalance(seller).toNumber();
        let result = await scoopMarket.withdrawPayments({ from: seller });
        let balanceAfter = web3.eth.getBalance(seller).toNumber();
        let tx = web3.eth.getTransaction(result.tx);
        let cost = tx.gasPrice.mul(result.receipt.gasUsed).toNumber();
        assert(balanceAfter = balanceBefore - cost + 1*10**14, "Seller withdrawed credit correctly.");
    });
    it("buyer should be able to request only token for sale.", async () => {
        checkReverted(
            scoopMarket.request(2, { from: buyer })
        );
    });
    it("buyer should be able to cancel request.", async () => {
        await scoopMarket.request(3, { from: buyer });
        // Cancel before approval.
        await scoopMarket.cancel(3, { from: buyer });
        checkReverted(
            // Can't approve canceled request.
            scoopMarket.approve(buyer, 3, { from: seller })
        );
    });
    it("buyer should be able to cancel approval.", async () => {
        await scoopMarket.request(4, { from: buyer });
        await scoopMarket.approve(buyer, 4, { from: seller });
        // Cancel after approval.
        await scoopMarket.cancel(4, { from: buyer });
        // Can't purchase canceled token.
        checkReverted(
            scoopMarket.purchase(4, { from: buyer, value: 5*10**14 })
        );
    });
    it("seller should be able to approve request.", async () => {
        await scoopMarket.request(5, { from: buyer });
        await scoopMarket.approve(buyer, 5, { from: seller });
        checkReverted(
            // Can't approve not requested token.
            scoopMarket.approve(buyer, 6, { from: seller })
        );
    });
    it("seller should be able to deny request.", async () => {
        await scoopMarket.request(6, { from: buyer });
        await scoopMarket.deny(6, { from: seller });
        // Can't purchase denied token.
        checkReverted(
            scoopMarket.purchase(6, { from: buyer, value: 7*10**14 })
        );
    });
});
