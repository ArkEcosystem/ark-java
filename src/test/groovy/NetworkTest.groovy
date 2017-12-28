/*
 * This Spock specification was auto generated by running 'gradle init --type groovy-library'
 */

import spock.lang.Specification

class NetworkTest extends Specification {

    def "Should connect to Mainnet"(){
      setup:
        Network.Mainnet.warmup()
        def peer = Network.Mainnet.randomPeer
      when:
        def status = peer.getStatus()
      then:
        status.currentSlot > status.height
    }

    // TODO: stub
    def "Should connect to Devnet"(){
      setup:
        Network.Devnet.warmup()
        def peer = Network.Devnet.randomPeer
      when:
        def status = peer.getStatus()
      then:
        status.currentSlot > status.height
    }

    // TODO: stub
    def "Should post a transaction to a Mainnet Peer"(){
      setup:
        Network.Mainnet.warmup()
        def peer = Network.Mainnet.randomPeer
      when:
        def transaction = Transaction.createTransaction("AXoXnFi4z1Z6aFvjEYkDVCtBGW2PaRiM25", 133380000000, "This is first transaction from JAVA", "this is a top secret passphrase")
        def result = peer << transaction
      then:
        result.error == "Account does not have enough ARK: AGeYmgbg2LgGxRW2vNNJvQ88PknEJsYizC balance: 0"
    }

    // TODO: stub
    def "Should post a vote transaction to a Mainnet Peer"(){
      setup:
      def peer = mainnet.randomPeer
      when:
      def transaction = Transaction.createVote(["+123456789"], "this is a top secret passphrase")
      def result = peer << transaction
      then:
      result.error == "Account does not have enough ARK: AGeYmgbg2LgGxRW2vNNJvQ88PknEJsYizC balance: 0"
    }

    // TODO: stub
    def "Should broadcast a transaction to Mainnet"(){
      setup:
        Network.Mainnet.warmup()
      when:
        def transaction = Transaction.createTransaction("AXoXnFi4z1Z6aFvjEYkDVCtBGW2PaRiM25", 133380000000, "This is first transaction from JAVA", "this is a top secret passphrase")
        def result = Network.Mainnet << transaction
      then:
        result == Network.Mainnet.broadcastMax
    }

}
