// package com.uit.accountservice.soap;

// import com.uit.accountservice.dto.AccountDto;
// import com.uit.accountservice.dto.request.TransferRequest;
// import com.uit.accountservice.dto.request.VerifyTransferRequest;
// import com.uit.accountservice.dto.response.ChallengeResponse;
// import com.uit.accountservice.service.AccountService;
// import com.uit.accountservice.service.TransferAuditService;
// import jakarta.xml.bind.JAXBElement;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.ws.context.MessageContext;

// import java.math.BigDecimal;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// @DisplayName("TransferEndpoint Unit Tests")
// class TransferEndpointTest {

//     @Mock
//     private AccountService accountService;

//     @Mock
//     private TransferAuditService auditService;

//     @Mock
//     private MessageContext messageContext;

//     @InjectMocks
//     private TransferEndpoint transferEndpoint;

//     @Test
//     @DisplayName("processTransfer() returns COMPLETED for low risk")
//     void testProcessTransfer_Completed() {
//         TransferEndpoint.TransferRequestType requestType = new TransferEndpoint.TransferRequestType();
//         requestType.setSenderAccountId("acc1");
//         requestType.setReceiverAccountId("acc2");
//         requestType.setAmount(BigDecimal.TEN);
        
//         JAXBElement<TransferEndpoint.TransferRequestType> request = new JAXBElement<>(
//                 new javax.xml.namespace.QName("TransferRequest"),
//                 TransferEndpoint.TransferRequestType.class,
//                 requestType
//         );

//         when(messageContext.getProperty("userId")).thenReturn("user1");
//         when(accountService.handleTransfer(any(TransferRequest.class), eq("user1"), any(), any(), any()))
//                 .thenReturn(new AccountDto());

//         JAXBElement<TransferEndpoint.TransferResponseType> response = 
//                 transferEndpoint.processTransfer(request, messageContext);

//         assertThat(response.getValue().getStatus()).isEqualTo("COMPLETED");
//     }

//     @Test
//     @DisplayName("processTransfer() returns CHALLENGE_REQUIRED for medium risk")
//     void testProcessTransfer_ChallengeRequired() {
//         TransferEndpoint.TransferRequestType requestType = new TransferEndpoint.TransferRequestType();
//         requestType.setSenderAccountId("acc1");
//         requestType.setReceiverAccountId("acc2");
//         requestType.setAmount(BigDecimal.TEN);
        
//         JAXBElement<TransferEndpoint.TransferRequestType> request = new JAXBElement<>(
//                 new javax.xml.namespace.QName("TransferRequest"),
//                 TransferEndpoint.TransferRequestType.class,
//                 requestType
//         );

//         when(messageContext.getProperty("userId")).thenReturn("user1");
        
//         ChallengeResponse challenge = new ChallengeResponse("CHALLENGE_REQUIRED", "id1", "SMS_OTP");
//         when(accountService.handleTransfer(any(TransferRequest.class), eq("user1"), any(), any(), any()))
//                 .thenReturn(challenge);

//         JAXBElement<TransferEndpoint.TransferResponseType> response = 
//                 transferEndpoint.processTransfer(request, messageContext);

//         assertThat(response.getValue().getStatus()).isEqualTo("CHALLENGE_REQUIRED");
//         assertThat(response.getValue().getChallengeId()).isEqualTo("id1");
//     }

//     @Test
//     @DisplayName("verifyTransfer() returns COMPLETED on success")
//     void testVerifyTransfer_Completed() {
//         TransferEndpoint.VerifyTransferRequestType requestType = new TransferEndpoint.VerifyTransferRequestType();
//         requestType.setChallengeId("id1");
//         requestType.setOtpCode("123456");
        
//         JAXBElement<TransferEndpoint.VerifyTransferRequestType> request = new JAXBElement<>(
//                 new javax.xml.namespace.QName("VerifyTransferRequest"),
//                 TransferEndpoint.VerifyTransferRequestType.class,
//                 requestType
//         );

//         when(messageContext.getProperty("userId")).thenReturn("user1");
        
//         AccountDto accountDto = new AccountDto();
//         accountDto.setBalance(BigDecimal.valueOf(1000));
//         when(accountService.verifyTransfer(any(VerifyTransferRequest.class))).thenReturn(accountDto);

//         JAXBElement<TransferEndpoint.VerifyTransferResponseType> response = 
//                 transferEndpoint.verifyTransfer(request, messageContext);

//         assertThat(response.getValue().getStatus()).isEqualTo("COMPLETED");
//         assertThat(response.getValue().getBalance()).isEqualTo(BigDecimal.valueOf(1000));
//     }
// }

