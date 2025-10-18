import React from 'react'

const ServicePolicy: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto px-6 py-4 text-gray-800 leading-relaxed">
      {/* Article 1 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 1 (Definitions)</h2>
      <p className="mb-4 font-semibold">
        The meanings of the terms used in these Terms and Conditions are as follows:
      </p>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>
          Member: Refers to a person who agrees to these Terms and Conditions and uses the Service.
        </li>
        <li>
          Service: Refers to the overall Korean language education services provided by the Company,
          including additional features such as chat, distance slider, and storage.
        </li>
        <li>
          AI Coach: Refers to an artificial intelligence system that corrects and recommends
          language expressions based on the Member’s messages.
        </li>
        <li>
          Distance Slider: Refers to an interface feature selected by the Member to adjust the sense
          of intimacy with the counterpart during a conversation.
        </li>
        <li>
          Storage: Refers to a space where the Member can store and manage desired messages or
          learning records during conversations with the AI Coach.
        </li>
        <li>
          Input / Output / Content / Account / MVP: Defined collectively in the Service usage
          context.
        </li>
      </ol>

      {/* Article 2 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 2 (Purpose)</h2>
      <p className="mb-6">
        The purpose of these Terms and Conditions is to stipulate the rights, obligations,
        responsibilities, and other necessary matters between [Company Name] (hereinafter “the
        Company”) and the Members regarding the use of the Korean language education service
        (hereinafter “the Service”).
      </p>

      {/* Article 3 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 3 (Membership Registration)</h2>
      <p className="mb-4">
        A user applies for membership registration by filling in personal information in the
        prescribed form and agreeing to these Terms. Once the Company accepts the application, the
        Service becomes available.
      </p>
      <p className="mb-2 font-semibold">
        The Company may refuse or terminate membership in the following cases:
      </p>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>If another person’s name is stolen or false information is provided.</li>
        <li>If there are false entries, omissions, or errors in the application.</li>
        <li>
          If the user intends to use the Service for illegal purposes or in violation of these
          Terms.
        </li>
      </ol>

      {/* Article 4 */}
      <h2 className="text-2xl font-extrabold mb-3">
        Article 4 (Collection and Use of Personal Information)
      </h2>
      <p>The Company collects the following data for service provision:</p>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>Last name</li>
        <li>First name</li>
        <li>E-mail</li>
        <li>Password</li>
        <li>Nickname</li>
      </ol>
      <p className="mb-4">
        The nickname is identical to the Last name entered during registration. All data are used
        for identity verification, service provision, and improvement, complying with the Personal
        Information Protection Act. Upon withdrawal, data are destroyed immediately according to
        law.
      </p>

      {/* Article 5 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 5 (Management of Member Information)</h2>
      <p className="mb-4">
        For system stability, Last name, First name, Nickname, and E-mail cannot be modified once
        set. Only the password is editable.
      </p>
      <p className="mb-6">
        The Member must manage their account safely and may not transfer or lend it to others.
      </p>

      {/* Article 6 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 6 (Use of Service)</h2>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>Text chat is available with limits (80 Korean / 150 English characters).</li>
        <li>AI Coach only corrects or recommends language expressions.</li>
        <li>Chatroom contents delete when closed, except those saved in Storage.</li>
        <li>AI Coaching is not professional advice; the Member bears responsibility for usage.</li>
      </ol>

      {/* Article 7 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 7 (Content)</h2>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>The Member retains ownership of both Input and Output.</li>
        <li>The Output may not be unique due to AI model characteristics.</li>
        <li>
          Content may be used for maintenance, improvement, and compliance unless the Member opts
          out.
        </li>
        <li>Output accuracy is the Member’s responsibility.</li>
      </ol>

      {/* Article 8 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 8 (Data Storage and Use)</h2>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>Chat data retained on servers for six months.</li>
        <li>De-identified conversation data may train AI models.</li>
        <li>Sensitive information (political, health, finance) is excluded.</li>
        <li>Consent withdrawal follows the Privacy Policy.</li>
      </ol>

      {/* Article 9 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 9 (Membership Withdrawal)</h2>
      <p className="mb-6">
        The Member may withdraw anytime via Service settings. Upon withdrawal, all data are
        destroyed per law.
      </p>

      {/* Article 10 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 10 (Termination of Service)</h2>
      <p className="mb-6">
        The Company may terminate or change the Service at its discretion. Upon termination, all
        rights of Members expire.
      </p>

      {/* Article 11 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 11 (Service Quality and Operation)</h2>
      <p className="mb-6">
        As the Service is in MVP phase, features may change. The Company may notify Members of
        issues via pop-up or e-mail.
      </p>

      {/* Article 12 */}
      <h2 className="text-2xl font-extrabold mb-3">Article 12 (Obligations of Members)</h2>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>Provide accurate information and comply with regulations.</li>
        <li>Do not engage in illegal acts, harm others, or disrupt Service operations.</li>
      </ol>

      {/* Article 13 */}
      <h2 className="text-2xl font-extrabold mb-3">
        Article 13 (Limitation of Liability of the Company)
      </h2>
      <ol className="list-decimal list-inside mb-6 space-y-1">
        <li>No liability for interruptions caused by natural disasters or network failures.</li>
        <li>No responsibility for issues arising from Member’s actions.</li>
        <li>No guarantee of Output accuracy or completeness.</li>
      </ol>

      {/* Article 14 */}
      <h2 className="text-2xl font-extrabold mb-3">
        Article 14 (Consent to and Changes of Terms and Conditions)
      </h2>
      <p>
        Members must agree to these Terms to use the Service. The Company may revise them with prior
        notice, effective after publication. Members who disagree may withdraw membership.
      </p>
    </div>
  )
}

export default ServicePolicy
