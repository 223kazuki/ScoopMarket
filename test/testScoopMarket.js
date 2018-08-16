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

contract('ScoopMarket', ([owner, user1, user2]) => {
    let scoopMarket;
    const testHash = "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqr";
    const zeroAddress = "0x0000000000000000000000000000000000000000";

    beforeEach('setup contract for each test', async function () {
        scoopMarket = await ScoopMarket.new({ from: owner });
    })

    // As a Proof of Existence dApp
    it("should mint scoops successfully.", async () => {
        await scoopMarket.mint("scoop0", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop2", 10**15, false, testHash, { from: user1 });

        let balance = await scoopMarket.balanceOf(user1);
        assert(balance.toNumber() === 3, "Balance is correct.");

        let tokens = await scoopMarket.scoopsOf(user1);
        assert(tokens[0].toNumber() === 0, "Token 0 ID is correct.");
        assert(tokens[1].toNumber() === 1, "Token 1 ID is correct.");
        assert(tokens[2].toNumber() === 2, "Token 2 ID is correct.");

        let tokensForSale = await scoopMarket.scoopsForSale();
        assert(tokensForSale[0], "Token 0 ID is for sale.");
        assert(tokensForSale[1], "Token 1 ID is for sale.");
        assert(!tokensForSale[2], "Token 2 ID is not for sale.");

        {
            let scoop0 = await scoopMarket.scoop(0);
            let [id, name, _, imageHash, price, forSale, metaDataHash, author, owner, requestor] = scoop0;
            assert(id.toNumber() === 0, "scoop0 id is correct.");
            assert(name === "scoop0", "scoop0 name is correct.");
            assert(imageHash === testHash, "scoop0 imageHash is correct.");
            assert(price.toNumber() === 10**15, "scoop0 price is correct.");
            assert(forSale, "scoop0 forSale is correct.");
            assert(metaDataHash === "", "scoop0 metaDataHash is correct.");
            assert(author === user1, "scoop0 author is correct.");
            assert(owner === user1, "scoop0 owner is correct.");
            assert(requestor === zeroAddress, "scoop0 requestor is correct.");
        }
        {
            let scoop1 = await scoopMarket.scoop(1);
            let [id, name, _, imageHash, price, forSale, metaDataHash, author, owner, requestor] = scoop1;
            assert(id.toNumber() === 1, "scoop1 id is correct.");
            assert(name === "scoop1", "scoop1 name is correct.");
            assert(imageHash === testHash, "scoop1 imageHash is correct.");
            assert(price.toNumber() === 10**15, "scoop1 price is correct.");
            assert(forSale, "scoop1 forSale is correct.");
            assert(metaDataHash === "", "scoop1 metaDataHash is correct.");
            assert(author === user1, "scoop1 author is correct.");
            assert(owner === user1, "scoop1 owner is correct.");
            assert(requestor === zeroAddress, "scoop1 requestor is correct.");
        }
        {
            let scoop2 = await scoopMarket.scoop(2);
            let [id, name, _, imageHash, price, forSale, metaDataHash, author, owner, requestor] = scoop2;
            assert(id.toNumber() === 2, "scoop2 id is correct.");
            assert(name === "scoop2", "scoop2 name is correct.");
            assert(imageHash === testHash, "scoop2 imageHash is correct.");
            assert(price.toNumber() === 10**15, "scoop2 price is correct.");
            assert(!forSale, "scoop2 forSale is correct.");
            assert(metaDataHash === "", "scoop2 metaDataHash is correct.");
            assert(author === user1, "scoop2 author is correct.");
            assert(owner === user1, "scoop2 owner is correct.");
            assert(requestor === zeroAddress, "scoop2 requestor is correct.");
        }
    });
    it("should not mint scoop with insufficient value.", async () => {
        // When MintCost is zero.
        checkReverted(
            // MintCost - 1
            scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: - 1 })
        );
        checkReverted(
            // MintCost + 1
            scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: 1 })
        );
        // When MintCost is positive.
        await scoopMarket.setMintCost(10**10, { from: owner });
        checkReverted(
            // MintCost - 1
            scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: 10**10 - 1 })
        );
        checkReverted(
            // MintCost + 1
            scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: 10**10 + 1 })
        );
    });
    it("only owner can set mint cost and withdraw it.", async () => {
        await scoopMarket.setMintCost(10**16, { from: owner });
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user1, value: 10**16 });
        checkReverted(
            scoopMarket.mint("scoop2", 10**15, true,  testHash, { from: user1 })
        );

        let balanceBefore = web3.eth.getBalance(owner).toNumber();
        let result = await scoopMarket.withdrawPayments({ from: owner });
        let balanceAfter = web3.eth.getBalance(owner).toNumber();

        let tx = web3.eth.getTransaction(result.tx);        
        let cost = tx.gasPrice.mul(result.receipt.gasUsed).toNumber();

        assert(balanceAfter = balanceBefore - cost + 10**16, "Owner withdrawed credit correctly.");


        
    });
    it("should not mint scoop with invalid input parameters.", async () => {
        // Name length must be between 1 to 50.
        // length = 1
        await scoopMarket.mint("a", 10**15, true,  testHash, { from: user1 });
        // length = 50
        await scoopMarket.mint("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 10**15, true,  testHash, { from: user1 });
        checkReverted(
            // length = 0
            scoopMarket.mint("", 10**15, true,  testHash, { from: user1 })
        );
        checkReverted(
            // length = 51
            scoopMarket.mint("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 10**15, true,  testHash, { from: user1 })
        );
        // Image hash length must be 46.
        checkReverted(
            // length = 45
            scoopMarket.mint("scoop1", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pq", { from: user1 })
        );
        checkReverted(
            // length = 47
            scoopMarket.mint("scoop2", 10**15, true,  "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqra", { from: user1 })
        );
    });
    it("should edit scoop successfully.", async () => {
        await scoopMarket.mint("scoop0", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.editToken(0, "scoop0changed", 10**14, false, testHash, { from: user1 });
        {
            let scoop0 = await scoopMarket.scoop(0);
            let [id, name, _, imageHash, price, forSale, metaDataHash, author, owner, requestor] = scoop0;
            assert(id.toNumber() === 0, "scoop0 id is correct.");
            assert(name === "scoop0changed", "scoop0 name is correct.");
            assert(imageHash === testHash, "scoop0 imageHash is correct.");
            assert(price.toNumber() === 10**14, "scoop0 price is correct.");
            assert(!forSale, "scoop0 forSale is correct.");
            assert(metaDataHash === testHash, "scoop0 metaDataHash is correct.");
            assert(author === user1, "scoop0 author is correct.");
            assert(owner === user1, "scoop0 owner is correct.");
            assert(requestor === zeroAddress, "scoop0 requestor is correct.");
        }
    });
    it("should not edit scoop with invalid input parameters.", async () => {
        await scoopMarket.mint("scoop0", 10**15, true,  testHash, { from: user1 });
        await scoopMarket.mint("scoop1", 10**15, true,  testHash, { from: user2 });
        // Token doesn't exist.
        checkReverted(
            scoopMarket.editToken(2, "scoop0changed", 10**14, false, testHash, { from: user1 })
        );
        // Token isn't mine.
        checkReverted(
            scoopMarket.editToken(1, "scoop0changed", 10**14, false, testHash, { from: user1 })
        );
        // Name length must be between 1 to 50
        // length = 1
        await scoopMarket.editToken(0, "a", 10**14, false, testHash, { from: user1 });
        // length = 50
        await scoopMarket.editToken(0, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 10**14, false, testHash, { from: user1 });
        checkReverted(
            // length = 0
            scoopMarket.editToken(0, "", 10**14, false, testHash, { from: user1 })
        );
        checkReverted(
            // length = 51
            scoopMarket.editToken(0, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 10**14, false, testHash, { from: user1 })
        );
        // Image hash length must be 46.
        checkReverted(
            // length = 45
            scoopMarket.editToken(0, "scoop0changed", 10**14, false, "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pq", { from: user1 })
        );
        checkReverted(
            // length = 47
            scoopMarket.editToken(0, "scoop0changed", 10**14, false, "QmSoPpGPFr3gz9rfwfwJuLahjTTmhdFJtKNvHYS58s8pqra", { from: user1 })
        );
    });
});
