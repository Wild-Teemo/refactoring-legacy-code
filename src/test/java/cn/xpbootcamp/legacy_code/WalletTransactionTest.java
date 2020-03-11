package cn.xpbootcamp.legacy_code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;
import java.util.UUID;
import javax.transaction.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WalletTransactionTest {

  @Mock
  private DistributedLock distributedLock;
  @Mock
  private WalletService walletService;

  @Test
  void should_return_true_when_execute_transaction_success() throws InvalidTransactionException {
    // given
    String preAssignedId = "t_" + UUID.randomUUID().toString();
    Long buyerId = 111L;
    Long sellerId = 111L;
    Long productId = 111L;
    String orderId = UUID.randomUUID().toString();
    Double amount = 11.1;
    given(distributedLock.lock(preAssignedId)).willReturn(true);
    given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount))
        .willReturn(UUID.randomUUID().toString());

    WalletTransaction walletTransaction =
        new WalletTransaction(preAssignedId, buyerId, sellerId, productId, orderId, amount);
    walletTransaction.setDistributedLock(distributedLock);
    walletTransaction.setWalletService(walletService);

    // when
    boolean executeResult = walletTransaction.execute();

    // then
    assertThat(executeResult).isTrue();
  }
}
