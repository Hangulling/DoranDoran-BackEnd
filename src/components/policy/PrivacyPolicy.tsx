const PrivacyPolicy: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto px-6 py-2 text-gray-800 leading-relaxed">
      <p className="mb-6">
        <span className="font-bold">DoranDoran </span>
        (“we,” “our,” or “the Company”) respects your privacy and is committed to protecting your
        personal information. This Privacy Policy describes how we collect, use, store, and protect
        your data in accordance with applicable privacy laws, including the Personal Information
        Protection Act of the Republic of Korea (“PIPA”) and other relevant international
        regulations.
      </p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">1. Information We Collect</h2>
      <h3 className="font-semibold mt-4 mb-2">Required Information</h3>
      <p>We collect the following personal data that are necessary to provide our services:</p>
      <ul className="list-disc list-inside mb-4">
        <li>During sign-up: First name, Last name, Nickname, Email address, Password</li>
        <li>
          During service use: Login history (timestamp, IP address, device info), access logs,
          cookies
        </li>
      </ul>

      <h3 className="font-semibold mt-4 mb-2">Optional Information</h3>
      <p>Collected only with your consent for enhanced user experience and service improvement:</p>
      <ul className="list-disc list-inside mb-4">
        <li>Chat history, learning activity data, and AI coaching logs</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">2. Purpose of Data Collection and Use</h2>
      <h3 className="font-semibold mt-4 mb-2">For Required Information</h3>
      <ul className="list-disc list-inside mb-4">
        <li>
          To register and authenticate members, manage logins, and provide core Korean learning
          features
        </li>
        <li>To ensure account security and maintain stable service operations</li>
      </ul>

      <h3 className="font-semibold mt-4 mb-2">For Optional Information</h3>
      <ul className="list-disc list-inside mb-4">
        <li>To offer personalized learning experiences and improve AI coaching quality</li>
        <li>
          To enhance overall service quality and perform research, analysis, and statistical
          reporting
        </li>
        <li>To provide marketing and promotional content (only with prior consent)</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">3. Data Retention and Deletion</h2>
      <ul className="list-disc list-inside mb-4">
        <li>Upon account deletion, data retained for 30 days then permanently deleted.</li>
        <li>Transaction or contract records: 5 years</li>
        <li>Consumer complaint and dispute records: 3 years</li>
        <li>Access logs: 6 months</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">
        4. Data Sharing and Cross-Border Transfers
      </h2>
      <ul className="list-disc list-inside mb-4">
        <li>
          We do not share personal information with third parties unless required by law or with
          your explicit consent.
        </li>
        <li>
          For infrastructure and data storage purposes, your information may be transferred and
          stored using cloud services such as Amazon Web Services, Inc. (“AWS”).
        </li>
      </ul>

      <p className="mb-4">Details of Cross-Border Transfer:</p>
      <ul className="list-disc list-inside mb-4">
        <li>Recipient: Amazon Web Services, Inc.</li>
        <li>
          Destination: United States, Japan, Singapore (additional backup regions may be added per
          our disaster recovery policy)
        </li>
        <li>
          Transferred Data: Account information (name, email, nickname), service usage records,
          access logs, AI coaching data
        </li>
        <li>
          Purpose: Service hosting, data storage and management, system operation, and service
          quality improvement
        </li>
        <li>Retention: Follows the retention periods outlined above</li>
      </ul>
      <p>
        Users may refuse consent for international data transfer; however, certain service features
        may become unavailable as a result.
      </p>
      <p>
        All international transfers comply with applicable data protection laws and security
        standards.
      </p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">5. Data Processing and Outsourcing</h2>
      <p>
        We may outsource limited processing tasks, such as server and database management, to
        trusted cloud service providers (e.g., AWS).
      </p>
      <p>
        All vendors are contractually bound to protect your personal information and comply with
        applicable privacy regulations.
      </p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">6. User Rights</h2>
      <p>You have the right to: </p>
      <ul className="list-disc list-inside mb-4">
        <li>Access, correct, or request deletion of personal information</li>
        <li>Withdraw consent for optional data collection</li>
      </ul>
      <p>Editable fields: Password</p>
      <p>Non-editable fields: First name, Last name, Nickname, Email (for fraud prevention)</p>
      <p>Upon account deletion, all personal data will be permanently erased.</p>
      <p>You may also request a copy of your data prior to deletion.</p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">7. Protection of Children’s Information</h2>
      <p>Dorandoran does not provide services to children under the age of 14.</p>
      <p>
        We do not knowingly collect personal data from minors without the consent of their legal
        guardian.
      </p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">8. Security Measures</h2>
      <p>
        We implement appropriate technical and administrative safeguards to protect your data,
        including:
      </p>
      <ul className="list-disc list-inside mb-4">
        <li>Password encryption</li>
        <li>Access control with minimum privilege principles</li>
        <li>Secure HTTPS communication</li>
        <li>Regular security audits and monitoring</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">9. Data Handling During Service Use</h2>
      <ul className="list-disc list-inside mb-4">
        <li>Login: Temporary session data deleted after use; logs retained for 6 months</li>
        <li>Logout: Temporary session data deleted; logs retained for 6 months</li>
        <li>Chat history: Retained for 6 months by default; extended only when legally required</li>
        <li>
          AI coaching data: Used in de-identified form for service improvement; consent withdrawal
          available
        </li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">10. Updates to This Policy</h2>
      <ul className="list-disc list-inside mb-4">
        <li>This Privacy Policy takes effect on 2025.10.24.</li>
        <li>We will notify users at least 7 days prior to any material changes.</li>
        <li>
          In case of significant changes (e.g., new data use purposes), we may request renewed
          consent.
        </li>
      </ul>
    </div>
  )
}

export default PrivacyPolicy
