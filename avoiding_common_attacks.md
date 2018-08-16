# Avoiding Common Attacks

Common Attacks - https://www.kingoftheether.com/contract-safety-checklist.html

## C1. Logic Bugs
I developed ScoopMarket contract in simple design.
So respectively it hardly has a critical bug.
And event if there is a critical bug, it can stop to execute functions emergencily.

## C2. Failed Sends
There are no loop execution in transactional function.
And each transactional funciton check its input parameters.

## C3. Recursive Calls
ScoopMarket doesn't have "recursive split" functionality.

## C4. Integer Arithmetic Overflow
ScoopMarket uses OpenZeppelin SafeMath library to avoid it.

## C5. Poison Data
The contract of ScoopMarket is not supposed to be called by other applications.

## C6. Exposed Functions
Auditing ABI to check which functions are exposed.

## C7. Exposed Secrets
There are not secrets data on the contract of ScoopMarket.

## C8. Denial of Service / Dust Spam
Same as C2.

## C9. Miner Vulnerabilities
ScoopMarket doesn't expect a precision of better than fifteen minutes or so from block timestamps.

## C10. Malicious Creator
The creator of the contract can only change the cost to mint.

## C11. Off-chain Safety
ScoopMarket is purely static web application and it doesn't store any sensitive data.

## C12. Cross-chain Replay Attacks
ScoopMarket is deployed only on Rinkeby test net.

## C13. Tx.Origin Problem
ScoopMarket doesn't use tx.origin.

## C14. Solidity Function Signatures and Fallback Data Collisions

## C15. Incorrect use of Cryptography
ScoopMarket doesn't use any Cryptography.

## C16. Gas Limits
Same as C2.

## C17. Stack Depth Exhaustion
ScoopMarket has only no external contract call.
