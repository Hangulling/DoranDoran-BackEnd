const PrivacyPolicy: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto px-6 py-2 text-gray-800 leading-relaxed">
      <p className="mb-6">
        Dorandoran (“we,” “our,” or “the Company”) respects your privacy and is committed to
        protecting your personal information. This Privacy Policy describes how we collect, use,
        store, and protect your data in accordance with applicable privacy laws, including the
        Personal Information Protection Act of the Republic of Korea (“PIPA”) and other relevant
        international regulations.
      </p>

      {/* Section 1 */}
      <h2 className="text-2xl font-semibold mt-10 mb-3">1. Information We Collect</h2>
      <h3 className="font-semibold mt-4 mb-2">Required Information</h3>
      <ul className="list-disc list-inside mb-4">
        <li>During sign-up: First name, Last name, Nickname, Email address, Password</li>
        <li>
          During service use: Login history (timestamp, IP address, device info), access logs,
          cookies
        </li>
      </ul>

      <h3 className="font-semibold mt-4 mb-2">Optional Information</h3>
      <ul className="list-disc list-inside mb-4">
        <li>Chat history, learning activity data, and AI coaching logs</li>
      </ul>

      {/* Section 2 */}
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

      {/* Section 3 */}
      <h2 className="text-2xl font-semibold mt-10 mb-3">3. Data Retention and Deletion</h2>
      <ul className="list-disc list-inside mb-4">
        <li>Upon account deletion, data retained for 30 days then permanently deleted.</li>
        <li>Transaction or contract records: 5 years</li>
        <li>Consumer complaint and dispute records: 3 years</li>
        <li>Access logs: 6 months</li>
      </ul>

      {/* Section 4 */}
      <h2 className="text-2xl font-semibold mt-10 mb-3">
        4. Data Sharing and Cross-Border Transfers
      </h2>
      <p className="mb-4">
        We do not share personal information with third parties unless required by law or with your
        explicit consent.
      </p>
      <ul className="list-disc list-inside mb-4">
        <li>Recipient: Amazon Web Services, Inc.</li>
        <li>Destination: United States, Japan, Singapore</li>
        <li>
          Transferred Data: Account information, service usage records, access logs, AI coaching
          data
        </li>
        <li>
          Purpose: Hosting, data storage, management, system operation, and quality improvement
        </li>
      </ul>
      <p>
        Users may refuse consent for international data transfer; however, some features may become
        unavailable.
      </p>

      {/* Section 5~10 */}
      <h2 className="text-2xl font-semibold mt-10 mb-3">5. Data Processing and Outsourcing</h2>
      <p>
        We may outsource limited processing tasks (e.g., server and database management) to trusted
        providers like AWS.
      </p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">6. User Rights</h2>
      <ul className="list-disc list-inside mb-4">
        <li>Access, correct, or request deletion of personal information</li>
        <li>Withdraw consent for optional data collection</li>
        <li>Edit password (non-editable fields: First name, Last name, Nickname, Email)</li>
        <li>Request data copy prior to deletion</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">7. Protection of Children’s Information</h2>
      <p>
        Dorandoran does not provide services to children under 14 and does not knowingly collect
        their data.
      </p>

      <h2 className="text-2xl font-semibold mt-10 mb-3">8. Security Measures</h2>
      <ul className="list-disc list-inside mb-4">
        <li>Password encryption</li>
        <li>Access control (minimum privilege)</li>
        <li>Secure HTTPS communication</li>
        <li>Regular security audits</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">9. Data Handling During Service Use</h2>
      <ul className="list-disc list-inside mb-4">
        <li>Login sessions deleted after use; logs retained 6 months</li>
        <li>Logout deletes sessions; logs retained 6 months</li>
        <li>Chat history retained 6 months; extended only when legally required</li>
        <li>AI coaching data used in de-identified form with consent withdrawal option</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-10 mb-3">10. Updates to This Policy</h2>
      <p>
        This Privacy Policy takes effect on [Effective Date]. Users will be notified at least 7 days
        before material changes.
      </p>
    </div>
  )
}

export default PrivacyPolicy
